package com.health.assistant.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.health.assistant.client.DeepSeekClient;
import com.health.assistant.common.constant.ReportConstants;
import com.health.assistant.common.exception.BizException;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.dto.chat.ChatSessionCreateRequest;
import com.health.assistant.entity.ChatMessage;
import com.health.assistant.entity.ChatSession;
import com.health.assistant.entity.ExamIndicator;
import com.health.assistant.entity.ExamReport;
import com.health.assistant.entity.ExerciseDaily;
import com.health.assistant.entity.ExerciseWorkout;
import com.health.assistant.entity.FamilyMember;
import com.health.assistant.entity.MemberAllergy;
import com.health.assistant.entity.MemberFamilyHistory;
import com.health.assistant.entity.MemberLifestyle;
import com.health.assistant.entity.MemberMedicalHistory;
import com.health.assistant.entity.MemberMedication;
import com.health.assistant.mapper.ChatMessageMapper;
import com.health.assistant.mapper.ChatSessionMapper;
import com.health.assistant.mapper.ExamIndicatorMapper;
import com.health.assistant.mapper.ExamReportMapper;
import com.health.assistant.mapper.ExerciseDailyMapper;
import com.health.assistant.mapper.ExerciseWorkoutMapper;
import com.health.assistant.mapper.FamilyMemberMapper;
import com.health.assistant.mapper.MemberAllergyMapper;
import com.health.assistant.mapper.MemberFamilyHistoryMapper;
import com.health.assistant.mapper.MemberLifestyleMapper;
import com.health.assistant.mapper.MemberMedicalHistoryMapper;
import com.health.assistant.mapper.MemberMedicationMapper;
import com.health.assistant.security.SecurityUtils;
import com.health.assistant.service.ChatService;
import com.health.assistant.support.AiLogRecorder;
import com.health.assistant.support.AiQuotaService;
import com.health.assistant.support.HealthProfileFormatter;
import com.health.assistant.support.MemberAccessChecker;
import com.health.assistant.vo.ChatMessageVO;
import com.health.assistant.vo.ChatSessionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final int HISTORY_LIMIT = 24;
    private static final int TITLE_MAX_LENGTH = 20;
    private static final double CHAT_TEMPERATURE = 0.7;
    private static final int CHAT_MAX_TOKENS = 4096;
    private static final long STREAM_TIMEOUT_MS = 180_000L;
    private static final String DEFAULT_TITLE = "新对话";

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final FamilyMemberMapper memberMapper;
    private final ExamReportMapper reportMapper;
    private final ExamIndicatorMapper indicatorMapper;
    private final MemberMedicalHistoryMapper medicalHistoryMapper;
    private final MemberAllergyMapper allergyMapper;
    private final MemberMedicationMapper medicationMapper;
    private final MemberFamilyHistoryMapper familyHistoryMapper;
    private final MemberLifestyleMapper lifestyleMapper;
    private final ExerciseWorkoutMapper workoutMapper;
    private final ExerciseDailyMapper dailyMapper;
    private final HealthProfileFormatter profileFormatter;
    private final MemberAccessChecker memberAccessChecker;
    private final DeepSeekClient deepSeekClient;
    private final AiLogRecorder aiLogRecorder;
    private final AiQuotaService aiQuotaService;

    private final Map<Long, Call> activeCalls = new ConcurrentHashMap<>();

    @Override
    public List<ChatSessionVO> listSessions(Long memberId) {
        Long userId = SecurityUtils.getCurrentUserId();
        memberAccessChecker.check(userId, memberId);
        return sessionMapper.selectList(Wrappers.<ChatSession>lambdaQuery()
                        .eq(ChatSession::getMemberId, memberId)
                        .eq(ChatSession::getUserId, userId)
                        .orderByDesc(ChatSession::getLastMessageAt)
                        .orderByDesc(ChatSession::getId))
                .stream()
                .map(ChatSessionVO::from)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatSessionVO createSession(ChatSessionCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        memberAccessChecker.check(userId, request.getMemberId());
        ChatSession session = new ChatSession();
        session.setMemberId(request.getMemberId());
        session.setUserId(userId);
        session.setTitle(StringUtils.hasText(request.getTitle()) ? request.getTitle() : DEFAULT_TITLE);
        session.setMessageCount(0);
        session.setLastMessageAt(LocalDateTime.now());
        sessionMapper.insert(session);
        return ChatSessionVO.from(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatSessionVO renameSession(Long id, String title) {
        ChatSession session = getOwnedSession(SecurityUtils.getCurrentUserId(), id);
        if (!StringUtils.hasText(title)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "标题不能为空");
        }
        session.setTitle(title.trim());
        sessionMapper.updateById(session);
        return ChatSessionVO.from(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long id) {
        ChatSession session = getOwnedSession(SecurityUtils.getCurrentUserId(), id);
        cancelActiveCall(session.getId());
        messageMapper.delete(Wrappers.<ChatMessage>lambdaQuery().eq(ChatMessage::getSessionId, id));
        sessionMapper.deleteById(session.getId());
    }

    @Override
    public List<ChatMessageVO> listMessages(Long sessionId) {
        getOwnedSession(SecurityUtils.getCurrentUserId(), sessionId);
        List<ChatMessage> messages = messageMapper.selectList(Wrappers.<ChatMessage>lambdaQuery()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT 200"));
        Collections.reverse(messages);
        return messages.stream().map(ChatMessageVO::from).toList();
    }

    @Override
    public SseEmitter streamMessage(Long sessionId, String content) {
        Long userId = SecurityUtils.getCurrentUserId();
        ChatSession session = getOwnedSession(userId, sessionId);
        if (activeCalls.containsKey(sessionId)) {
            throw new BizException(ErrorCode.CHAT_SESSION_BUSY);
        }
        if (!StringUtils.hasText(content)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "消息内容不能为空");
        }
        aiQuotaService.checkQuota(userId);
        ChatMessage userMessage = saveMessage(session, userId, "USER", content.trim(), "DONE", null, null);
        List<ChatMessage> history = loadHistory(sessionId);
        String systemPrompt = buildSystemPrompt(session);
        String userPrompt = buildHistoryPrompt(history);

        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        StringBuilder assistantContent = new StringBuilder();
        long start = System.currentTimeMillis();

        Call call = deepSeekClient.streamChat(
                new DeepSeekClient.ChatRequest(systemPrompt, userPrompt, CHAT_TEMPERATURE, CHAT_MAX_TOKENS, false),
                new DeepSeekClient.StreamListener() {
                    @Override
                    public void onDelta(String delta) {
                        assistantContent.append(delta);
                        sendEvent(emitter, "delta", Map.of("content", delta));
                    }

                    @Override
                    public void onComplete(DeepSeekClient.StreamResult result) {
                        try {
                            ChatMessage assistant = saveMessage(session, userId, "ASSISTANT",
                                    assistantContent.toString(), "DONE", result, (int) result.durationMs());
                            aiLogRecorder.recordStreamSuccess(userId, null, "CHAT", result);
                            refreshSession(session, userMessage);
                            Map<String, Object> done = new LinkedHashMap<>();
                            done.put("messageId", assistant.getId());
                            done.put("tokenIn", result.promptTokens());
                            done.put("tokenOut", result.completionTokens());
                            done.put("durationMs", result.durationMs());
                            sendEvent(emitter, "done", done);
                        } catch (Exception e) {
                            log.error("failed to save assistant message", e);
                            sendEvent(emitter, "error", Map.of("code", 10301, "message", "保存回复失败"));
                        } finally {
                            activeCalls.remove(sessionId);
                            emitter.complete();
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        String message = error.getMessage() == null ? "AI 服务异常" : error.getMessage();
                        aiLogRecorder.recordFailure(userId, null, "CHAT", "10301", message,
                                System.currentTimeMillis() - start);
                        try {
                            ChatMessage failed = saveMessage(session, userId, "ASSISTANT",
                                    assistantContent.toString(), "FAILED", null, null);
                            failed.setErrorMsg(message);
                            messageMapper.updateById(failed);
                        } catch (Exception ignored) {
                            // ignore
                        }
                        sendEvent(emitter, "error", Map.of("code", 10301, "message", message));
                        activeCalls.remove(sessionId);
                        emitter.complete();
                    }
                });
        if (call != null) {
            activeCalls.put(sessionId, call);
        }
        emitter.onCompletion(() -> cancelActiveCall(sessionId));
        emitter.onTimeout(() -> {
            cancelActiveCall(sessionId);
            emitter.complete();
        });
        emitter.onError(error -> cancelActiveCall(sessionId));
        return emitter;
    }

    private void cancelActiveCall(Long sessionId) {
        Call call = activeCalls.remove(sessionId);
        if (call != null) {
            call.cancel();
        }
    }

    private void sendEvent(SseEmitter emitter, String name, Map<String, Object> data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            log.debug("sse send failed: {}", e.getMessage());
        }
    }

    private ChatSession getOwnedSession(Long userId, Long sessionId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.CHAT_SESSION_NOT_FOUND);
        }
        return session;
    }

    private ChatMessage saveMessage(ChatSession session, Long userId, String role, String content,
                                    String status, DeepSeekClient.StreamResult result, Integer durationMs) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(session.getId());
        message.setMemberId(session.getMemberId());
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(content);
        message.setStatus(status);
        if (result != null) {
            message.setTokenIn(result.promptTokens());
            message.setTokenOut(result.completionTokens());
        }
        if (durationMs != null) {
            message.setDurationMs(durationMs);
        }
        messageMapper.insert(message);
        return message;
    }

    private void refreshSession(ChatSession session, ChatMessage firstUserMessage) {
        Long count = messageMapper.selectCount(Wrappers.<ChatMessage>lambdaQuery()
                .eq(ChatMessage::getSessionId, session.getId()));
        session.setMessageCount(count == null ? 0 : count.intValue());
        session.setLastMessageAt(LocalDateTime.now());
        if (!StringUtils.hasText(session.getTitle()) || DEFAULT_TITLE.equals(session.getTitle())) {
            String title = firstUserMessage.getContent().trim();
            session.setTitle(title.length() <= TITLE_MAX_LENGTH ? title : title.substring(0, TITLE_MAX_LENGTH));
        }
        sessionMapper.updateById(session);
    }

    private List<ChatMessage> loadHistory(Long sessionId) {
        List<ChatMessage> messages = new ArrayList<>(messageMapper.selectList(Wrappers.<ChatMessage>lambdaQuery()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT " + HISTORY_LIMIT)));
        Collections.reverse(messages);
        return messages;
    }

    private String buildHistoryPrompt(List<ChatMessage> history) {
        StringBuilder builder = new StringBuilder();
        if (history.size() > 1) {
            builder.append("【最近对话记录】\n");
            for (int i = 0; i < history.size() - 1; i++) {
                ChatMessage message = history.get(i);
                if ("USER".equals(message.getRole())) {
                    builder.append("用户：").append(message.getContent()).append("\n");
                } else if ("ASSISTANT".equals(message.getRole())) {
                    builder.append("助手：").append(message.getContent()).append("\n");
                }
            }
            builder.append("\n");
        }
        builder.append("用户的最新问题：").append(history.get(history.size() - 1).getContent());
        return builder.toString();
    }

    private String buildSystemPrompt(ChatSession session) {
        FamilyMember member = memberMapper.selectById(session.getMemberId());
        if (member == null) {
            throw new BizException(ErrorCode.MEMBER_NOT_FOUND);
        }
        StringBuilder builder = new StringBuilder();
        builder.append("你是\"健康小助手\"，一位耐心、专业的健康顾问，正在与用户讨论【")
                .append(member.getName()).append("】的健康情况。\n\n");
        builder.append("【成员画像】\n").append(profileFormatter.formatProfile(member)).append("\n\n");

        builder.append("【最近一次体检】\n");
        ExamReport latestReport = reportMapper.selectOne(Wrappers.<ExamReport>lambdaQuery()
                .eq(ExamReport::getMemberId, member.getId())
                .orderByDesc(ExamReport::getReportDate)
                .orderByDesc(ExamReport::getId)
                .last("LIMIT 1"));
        if (latestReport == null) {
            builder.append("暂无体检记录。\n\n");
        } else {
            List<ExamIndicator> indicators = indicatorMapper.selectList(Wrappers.<ExamIndicator>lambdaQuery()
                    .eq(ExamIndicator::getReportId, latestReport.getId())
                    .orderByAsc(ExamIndicator::getSort));
            List<ExamIndicator> abnormal = indicators.stream()
                    .filter(item -> ReportConstants.ABNORMAL_JUDGE_STATUSES.contains(item.getJudgeStatus()))
                    .toList();
            builder.append(latestReport.getReportDate());
            if (StringUtils.hasText(latestReport.getInstitution())) {
                builder.append(" ").append(latestReport.getInstitution());
            }
            builder.append("：");
            if (abnormal.isEmpty()) {
                builder.append("未发现明显异常指标。\n\n");
            } else {
                builder.append("异常指标 ").append(abnormal.size()).append(" 项：\n");
                for (ExamIndicator item : abnormal) {
                    builder.append("- ").append(item.getMetricName()).append(" ")
                            .append(item.getValueText() == null ? "" : item.getValueText());
                    if (StringUtils.hasText(item.getUnit())) {
                        builder.append(item.getUnit());
                    }
                    builder.append("（参考 ").append(StringUtils.hasText(item.getRefRangeText())
                            ? item.getRefRangeText() : "未标注").append("，")
                            .append(judgeLabel(item.getJudgeStatus())).append("）\n");
                }
                builder.append("\n");
            }
        }
        builder.append("【既往病史】").append(profileFormatter.formatMedicalHistories(
                medicalHistoryMapper.selectList(Wrappers.<MemberMedicalHistory>lambdaQuery()
                        .eq(MemberMedicalHistory::getMemberId, member.getId())))).append("\n");
        builder.append("【过敏史】").append(profileFormatter.formatAllergies(
                allergyMapper.selectList(Wrappers.<MemberAllergy>lambdaQuery()
                        .eq(MemberAllergy::getMemberId, member.getId())))).append("\n");
        builder.append("【长期用药】").append(profileFormatter.formatMedications(
                medicationMapper.selectList(Wrappers.<MemberMedication>lambdaQuery()
                        .eq(MemberMedication::getMemberId, member.getId())))).append("\n");
        builder.append("【家族史】").append(profileFormatter.formatFamilyHistories(
                familyHistoryMapper.selectList(Wrappers.<MemberFamilyHistory>lambdaQuery()
                        .eq(MemberFamilyHistory::getMemberId, member.getId())))).append("\n");
        builder.append("【生活习惯】").append(profileFormatter.formatLifestyle(
                lifestyleMapper.selectOne(Wrappers.<MemberLifestyle>lambdaQuery()
                        .eq(MemberLifestyle::getMemberId, member.getId())))).append("\n\n");

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(29);
        List<ExerciseWorkout> workouts = workoutMapper.selectList(Wrappers.<ExerciseWorkout>lambdaQuery()
                .eq(ExerciseWorkout::getMemberId, member.getId())
                .ge(ExerciseWorkout::getWorkoutDate, start)
                .le(ExerciseWorkout::getWorkoutDate, end));
        List<ExerciseDaily> dailies = dailyMapper.selectList(Wrappers.<ExerciseDaily>lambdaQuery()
                .eq(ExerciseDaily::getMemberId, member.getId())
                .ge(ExerciseDaily::getRecordDate, start)
                .le(ExerciseDaily::getRecordDate, end)
                .orderByDesc(ExerciseDaily::getRecordDate));
        builder.append("【最近 30 天运动】");
        if (workouts.isEmpty() && dailies.isEmpty()) {
            builder.append("暂无运动记录。\n\n");
        } else {
            long totalMinutes = workouts.stream()
                    .mapToLong(item -> item.getDurationMin() == null ? 0 : item.getDurationMin())
                    .sum();
            int avgSteps = dailies.isEmpty() ? 0 : (int) dailies.stream()
                    .mapToInt(item -> item.getSteps() == null ? 0 : item.getSteps())
                    .average().orElse(0);
            builder.append("训练 ").append(workouts.size()).append(" 次，共 ").append(totalMinutes)
                    .append(" 分钟；日均步数 ").append(avgSteps).append(" 步。");
            dailies.stream().filter(item -> item.getWeightKg() != null).findFirst()
                    .ifPresent(item -> builder.append("最近体重 ")
                            .append(item.getWeightKg().stripTrailingZeros().toPlainString()).append("kg。"));
            dailies.stream().filter(item -> item.getSystolicBp() != null).findFirst()
                    .ifPresent(item -> builder.append("最近血压 ").append(item.getSystolicBp())
                            .append("/").append(item.getDiastolicBp()).append("mmHg。"));
            dailies.stream().filter(item -> item.getBloodGlucose() != null).findFirst()
                    .ifPresent(item -> builder.append("最近血糖 ")
                            .append(item.getBloodGlucose().stripTrailingZeros().toPlainString())
                            .append("mmol/L。"));
            builder.append("\n\n");
        }

        builder.append("【对话规则】\n");
        builder.append("1. 只讨论健康、运动、饮食、体检相关话题，其他话题礼貌拉回；\n");
        builder.append("2. 基于上方数据回答，引用时说明数据来源（如\"根据 2026-08-20 的体检报告\"）；\n");
        builder.append("3. 数据中没有的信息，明确说明档案中没有这项记录，不要编造；\n");
        builder.append("4. 不做诊断、不开处方；紧急症状（胸痛、呼吸困难、大出血等）立即建议拨打 120 就医；\n");
        builder.append("5. 用药相关问题一律引导\"请咨询开药医生或药师\"；\n");
        builder.append("6. 回答结构清晰，适当使用列表；回答末尾附一句简短免责声明。");
        return builder.toString();
    }

    private String judgeLabel(String status) {
        return switch (status == null ? "" : status) {
            case "HIGH" -> "偏高";
            case "LOW" -> "偏低";
            case "ABNORMAL" -> "异常";
            case "POSITIVE" -> "阳性";
            case "NEGATIVE" -> "阴性";
            default -> status;
        };
    }
}
