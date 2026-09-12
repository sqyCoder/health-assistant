package com.health.assistant.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.assistant.client.DeepSeekClient;
import com.health.assistant.client.DeepSeekProperties;
import com.health.assistant.common.constant.ReportConstants;
import com.health.assistant.common.exception.BizException;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.entity.AiAsyncTask;
import com.health.assistant.entity.AiInterpretation;
import com.health.assistant.entity.ExamIndicator;
import com.health.assistant.entity.ExamReport;
import com.health.assistant.entity.FamilyMember;
import com.health.assistant.mapper.AiInterpretationMapper;
import com.health.assistant.mapper.ExamIndicatorMapper;
import com.health.assistant.mapper.ExamReportMapper;
import com.health.assistant.mapper.FamilyMemberMapper;
import com.health.assistant.mapper.MemberAllergyMapper;
import com.health.assistant.mapper.MemberFamilyHistoryMapper;
import com.health.assistant.mapper.MemberLifestyleMapper;
import com.health.assistant.mapper.MemberMedicalHistoryMapper;
import com.health.assistant.mapper.MemberMedicationMapper;
import com.health.assistant.service.impl.PromptTemplateService;
import com.health.assistant.support.AiJsonUtils;
import com.health.assistant.support.AiLogRecorder;
import com.health.assistant.support.HealthProfileFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiInterpretTaskHandler implements TaskHandler {

    public static final String TASK_TYPE = "AI_INTERPRET";

    private final AiInterpretationMapper interpretationMapper;
    private final ExamReportMapper reportMapper;
    private final ExamIndicatorMapper indicatorMapper;
    private final FamilyMemberMapper memberMapper;
    private final MemberMedicalHistoryMapper medicalHistoryMapper;
    private final MemberAllergyMapper allergyMapper;
    private final MemberMedicationMapper medicationMapper;
    private final MemberFamilyHistoryMapper familyHistoryMapper;
    private final MemberLifestyleMapper lifestyleMapper;
    private final HealthProfileFormatter profileFormatter;
    private final PromptTemplateService promptTemplateService;
    private final DeepSeekClient deepSeekClient;
    private final DeepSeekProperties deepSeekProperties;
    private final AiLogRecorder aiLogRecorder;
    private final ObjectMapper objectMapper;

    @Override
    public String getTaskType() {
        return TASK_TYPE;
    }

    @Override
    public String handle(AiAsyncTask task) {
        AiInterpretation interpretation = interpretationMapper.selectById(task.getBizId());
        if (interpretation == null) {
            throw new BizException(ErrorCode.AI_GENERATE_FAILED, "解读记录不存在");
        }
        ExamReport report = reportMapper.selectById(interpretation.getReportId());
        if (report == null) {
            throw new BizException(ErrorCode.REPORT_NOT_FOUND);
        }
        FamilyMember member = memberMapper.selectById(interpretation.getMemberId());
        if (member == null) {
            throw new BizException(ErrorCode.MEMBER_NOT_FOUND);
        }
        List<ExamIndicator> indicators = indicatorMapper.selectList(Wrappers.<ExamIndicator>lambdaQuery()
                .eq(ExamIndicator::getReportId, report.getId())
                .orderByAsc(ExamIndicator::getSort));
        if (indicators.isEmpty()) {
            fail(interpretation, report, "报告没有指标，无法解读");
            throw new BizException(ErrorCode.AI_GENERATE_FAILED, "报告没有指标，无法解读");
        }

        try {
            PromptTemplateService.RenderedPrompt prompt = buildPrompt(member, indicators);
            long start = System.currentTimeMillis();
            DeepSeekClient.ChatResult result = deepSeekClient.chat(new DeepSeekClient.ChatRequest(
                    prompt.content(), "请按系统提示的要求，对以上体检指标进行解读。", prompt.temperature(), prompt.maxTokens(), true));
            aiLogRecorder.recordSuccess(task.getUserId(), task.getId(), "INTERPRET", result);
            try {
                saveResult(interpretation, result, prompt, indicators);
            } catch (IOException | RuntimeException first) {
                log.warn("interpretation json invalid, retrying: {}", first.getMessage());
                DeepSeekClient.ChatResult retry = deepSeekClient.chat(new DeepSeekClient.ChatRequest(
                        prompt.content(),
                        "请按系统提示的要求解读。注意：上次输出不是合法 JSON，请严格只输出 JSON 对象，不要任何解释或 markdown 代码块。",
                        prompt.temperature(), prompt.maxTokens(), true));
                aiLogRecorder.recordSuccess(task.getUserId(), task.getId(), "INTERPRET", retry);
                try {
                    saveResult(interpretation, retry, prompt, indicators);
                } catch (IOException e) {
                    throw new BizException(ErrorCode.AI_FORMAT_ERROR, "AI 返回格式异常: " + e.getMessage());
                }
            }
            report.setAiStatus("DONE");
            reportMapper.updateById(report);
            return objectMapper.writeValueAsString(Map.of("interpretationId", interpretation.getId()));
        } catch (Exception e) {
            fail(interpretation, report, e.getMessage());
            if (e instanceof BizException bizException) {
                throw bizException;
            }
            throw new BizException(ErrorCode.AI_GENERATE_FAILED, "AI 解读失败: " + e.getMessage());
        }
    }

    private PromptTemplateService.RenderedPrompt buildPrompt(FamilyMember member, List<ExamIndicator> indicators) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("memberProfile", profileFormatter.formatProfile(member));
        variables.put("indicators", profileFormatter.formatIndicators(indicators));
        variables.put("medicalHistory", profileFormatter.formatMedicalHistories(
                medicalHistoryMapper.selectList(Wrappers.<com.health.assistant.entity.MemberMedicalHistory>lambdaQuery()
                        .eq(com.health.assistant.entity.MemberMedicalHistory::getMemberId, member.getId()))));
        variables.put("allergy", profileFormatter.formatAllergies(
                allergyMapper.selectList(Wrappers.<com.health.assistant.entity.MemberAllergy>lambdaQuery()
                        .eq(com.health.assistant.entity.MemberAllergy::getMemberId, member.getId()))));
        variables.put("medication", profileFormatter.formatMedications(
                medicationMapper.selectList(Wrappers.<com.health.assistant.entity.MemberMedication>lambdaQuery()
                        .eq(com.health.assistant.entity.MemberMedication::getMemberId, member.getId()))));
        variables.put("familyHistory", profileFormatter.formatFamilyHistories(
                familyHistoryMapper.selectList(Wrappers.<com.health.assistant.entity.MemberFamilyHistory>lambdaQuery()
                        .eq(com.health.assistant.entity.MemberFamilyHistory::getMemberId, member.getId()))));
        variables.put("lifestyle", profileFormatter.formatLifestyle(
                lifestyleMapper.selectOne(Wrappers.<com.health.assistant.entity.MemberLifestyle>lambdaQuery()
                        .eq(com.health.assistant.entity.MemberLifestyle::getMemberId, member.getId()))));
        return promptTemplateService.render("AI_INTERPRET", variables);
    }

    private void saveResult(AiInterpretation interpretation, DeepSeekClient.ChatResult result,
                            PromptTemplateService.RenderedPrompt prompt, List<ExamIndicator> indicators) throws IOException {
        JsonNode root = objectMapper.readTree(AiJsonUtils.stripCodeFences(result.content()));
        JsonNode indicatorsNode = root.path("indicators");
        if (!indicatorsNode.isArray() || indicatorsNode.isEmpty()) {
            throw new IOException("响应缺少 indicators 数组");
        }
        for (JsonNode node : indicatorsNode) {
            if (!StringUtils.hasText(node.path("name").asText())
                    || !StringUtils.hasText(node.path("plainExplain").asText())) {
                throw new IOException("指标缺少 name 或 plainExplain 字段");
            }
        }
        List<String> focusMetrics = new ArrayList<>();
        JsonNode focusNode = root.path("focusMetrics");
        if (focusNode.isArray()) {
            focusNode.forEach(node -> focusMetrics.add(node.asText()));
        }
        int abnormalCount = (int) indicators.stream()
                .filter(item -> ReportConstants.ABNORMAL_JUDGE_STATUSES.contains(item.getJudgeStatus()))
                .count();

        interpretation.setRiskLevel(root.path("riskLevel").asText("MEDIUM"));
        interpretation.setSummary(root.path("summary").asText(""));
        interpretation.setContentJson(objectMapper.writeValueAsString(root));
        interpretation.setContentMd(root.path("summary").asText(""));
        interpretation.setFocusMetrics(objectMapper.writeValueAsString(focusMetrics));
        interpretation.setAbnormalCount(abnormalCount);
        interpretation.setStatus("DONE");
        interpretation.setModel(deepSeekProperties.getModel());
        interpretation.setPromptVersion(prompt.version());
        interpretation.setTokenIn(result.promptTokens());
        interpretation.setTokenOut(result.completionTokens());
        interpretation.setDurationMs((int) result.durationMs());
        interpretation.setErrorMsg(null);
        interpretationMapper.updateById(interpretation);
    }

    private void fail(AiInterpretation interpretation, ExamReport report, String message) {
        interpretation.setStatus("FAILED");
        interpretation.setErrorMsg(message == null ? "unknown" : message.substring(0, Math.min(message.length(), 1000)));
        interpretationMapper.updateById(interpretation);
        report.setAiStatus("FAILED");
        reportMapper.updateById(report);
        log.warn("interpretation failed: id={}, reason={}", interpretation.getId(), message);
    }
}
