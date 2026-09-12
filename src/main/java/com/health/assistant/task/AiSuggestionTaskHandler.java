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
import com.health.assistant.entity.AiSuggestion;
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
import com.health.assistant.mapper.AiSuggestionMapper;
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
import com.health.assistant.service.impl.PromptTemplateService;
import com.health.assistant.support.AiJsonUtils;
import com.health.assistant.support.AiLogRecorder;
import com.health.assistant.support.HealthProfileFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiSuggestionTaskHandler implements TaskHandler {

    public static final String TASK_TYPE = "AI_SUGGESTION";

    private final AiSuggestionMapper suggestionMapper;
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
        AiSuggestion suggestion = suggestionMapper.selectById(task.getBizId());
        if (suggestion == null) {
            throw new BizException(ErrorCode.AI_GENERATE_FAILED, "建议记录不存在");
        }
        FamilyMember member = memberMapper.selectById(suggestion.getMemberId());
        if (member == null) {
            throw new BizException(ErrorCode.MEMBER_NOT_FOUND);
        }
        try {
            Map<String, String> variables = new LinkedHashMap<>();
            variables.put("memberProfile", profileFormatter.formatProfile(member));
            variables.put("healthSummary", buildHealthSummary(member));
            variables.put("exerciseSummary", buildExerciseSummary(member));
            variables.put("goal", goalLabel(suggestion.getGoal()));
            PromptTemplateService.RenderedPrompt prompt =
                    promptTemplateService.render("AI_SUGGESTION", variables);

            DeepSeekClient.ChatResult result = deepSeekClient.chat(new DeepSeekClient.ChatRequest(
                    prompt.content(), "请按要求生成锻炼与康复建议。", prompt.temperature(), prompt.maxTokens(), true));
            aiLogRecorder.recordSuccess(task.getUserId(), task.getId(), "SUGGESTION", result);
            try {
                saveResult(suggestion, result, prompt);
            } catch (IOException | RuntimeException first) {
                log.warn("suggestion json invalid, retrying: {}", first.getMessage());
                DeepSeekClient.ChatResult retry = deepSeekClient.chat(new DeepSeekClient.ChatRequest(
                        prompt.content(),
                        "请按要求生成建议。注意：上次输出不是合法 JSON，请严格只输出 JSON 对象，不要任何解释或 markdown 代码块。",
                        prompt.temperature(), prompt.maxTokens(), true));
                aiLogRecorder.recordSuccess(task.getUserId(), task.getId(), "SUGGESTION", retry);
                try {
                    saveResult(suggestion, retry, prompt);
                } catch (IOException e) {
                    throw new BizException(ErrorCode.AI_FORMAT_ERROR, "AI 返回格式异常: " + e.getMessage());
                }
            }
            return objectMapper.writeValueAsString(Map.of("suggestionId", suggestion.getId()));
        } catch (Exception e) {
            suggestion.setStatus("FAILED");
            suggestion.setErrorMsg(e.getMessage() == null ? "unknown"
                    : e.getMessage().substring(0, Math.min(e.getMessage().length(), 1000)));
            suggestionMapper.updateById(suggestion);
            if (e instanceof BizException bizException) {
                throw bizException;
            }
            throw new BizException(ErrorCode.AI_GENERATE_FAILED, "AI 建议生成失败: " + e.getMessage());
        }
    }

    private String buildHealthSummary(FamilyMember member) {
        StringBuilder builder = new StringBuilder();
        ExamReport latestReport = reportMapper.selectOne(Wrappers.<ExamReport>lambdaQuery()
                .eq(ExamReport::getMemberId, member.getId())
                .orderByDesc(ExamReport::getReportDate)
                .orderByDesc(ExamReport::getId)
                .last("LIMIT 1"));
        if (latestReport == null) {
            builder.append("暂无体检报告数据。");
        } else {
            List<ExamIndicator> indicators = indicatorMapper.selectList(Wrappers.<ExamIndicator>lambdaQuery()
                    .eq(ExamIndicator::getReportId, latestReport.getId())
                    .orderByAsc(ExamIndicator::getSort));
            List<ExamIndicator> abnormal = indicators.stream()
                    .filter(item -> ReportConstants.ABNORMAL_JUDGE_STATUSES.contains(item.getJudgeStatus()))
                    .toList();
            builder.append("最近体检（").append(latestReport.getReportDate()).append("）：");
            if (abnormal.isEmpty()) {
                builder.append("未发现明显异常指标。");
            } else {
                builder.append("异常指标 ").append(abnormal.size()).append(" 项：");
                for (ExamIndicator item : abnormal) {
                    builder.append(item.getMetricName()).append(" ")
                            .append(item.getValueText() == null ? "" : item.getValueText());
                    if (StringUtils.hasText(item.getUnit())) {
                        builder.append(item.getUnit());
                    }
                    builder.append("（").append(judgeLabel(item.getJudgeStatus())).append("）；");
                }
            }
        }
        builder.append("\n既往病史：").append(profileFormatter.formatMedicalHistories(
                medicalHistoryMapper.selectList(Wrappers.<MemberMedicalHistory>lambdaQuery()
                        .eq(MemberMedicalHistory::getMemberId, member.getId()))));
        builder.append("\n过敏史：").append(profileFormatter.formatAllergies(
                allergyMapper.selectList(Wrappers.<MemberAllergy>lambdaQuery()
                        .eq(MemberAllergy::getMemberId, member.getId()))));
        builder.append("\n长期用药：").append(profileFormatter.formatMedications(
                medicationMapper.selectList(Wrappers.<MemberMedication>lambdaQuery()
                        .eq(MemberMedication::getMemberId, member.getId()))));
        builder.append("\n家族史：").append(profileFormatter.formatFamilyHistories(
                familyHistoryMapper.selectList(Wrappers.<MemberFamilyHistory>lambdaQuery()
                        .eq(MemberFamilyHistory::getMemberId, member.getId()))));
        builder.append("\n生活习惯：").append(profileFormatter.formatLifestyle(
                lifestyleMapper.selectOne(Wrappers.<MemberLifestyle>lambdaQuery()
                        .eq(MemberLifestyle::getMemberId, member.getId()))));
        return builder.toString();
    }

    private String buildExerciseSummary(FamilyMember member) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(29);
        List<ExerciseWorkout> workouts = workoutMapper.selectList(Wrappers.<ExerciseWorkout>lambdaQuery()
                .eq(ExerciseWorkout::getMemberId, member.getId())
                .ge(ExerciseWorkout::getWorkoutDate, start)
                .le(ExerciseWorkout::getWorkoutDate, end));
        long totalMinutes = workouts.stream()
                .mapToLong(item -> item.getDurationMin() == null ? 0 : item.getDurationMin())
                .sum();

        List<ExerciseDaily> dailies = dailyMapper.selectList(Wrappers.<ExerciseDaily>lambdaQuery()
                .eq(ExerciseDaily::getMemberId, member.getId())
                .ge(ExerciseDaily::getRecordDate, start)
                .le(ExerciseDaily::getRecordDate, end)
                .orderByDesc(ExerciseDaily::getRecordDate));
        int avgSteps = dailies.isEmpty() ? 0
                : (int) dailies.stream()
                        .mapToInt(item -> item.getSteps() == null ? 0 : item.getSteps())
                        .average().orElse(0);

        StringBuilder builder = new StringBuilder();
        if (workouts.isEmpty() && dailies.isEmpty()) {
            builder.append("近 30 天没有运动记录，运动基础较弱。");
        } else {
            builder.append("近 30 天：训练 ").append(workouts.size()).append(" 次，共 ")
                    .append(totalMinutes).append(" 分钟；日均步数 ").append(avgSteps).append(" 步。");
            ExerciseDaily latestWeight = dailies.stream()
                    .filter(item -> item.getWeightKg() != null).findFirst().orElse(null);
            if (latestWeight != null) {
                builder.append("最近体重 ").append(latestWeight.getWeightKg().stripTrailingZeros().toPlainString()).append("kg。");
            }
            ExerciseDaily latestBp = dailies.stream()
                    .filter(item -> item.getSystolicBp() != null).findFirst().orElse(null);
            if (latestBp != null) {
                builder.append("最近血压 ").append(latestBp.getSystolicBp()).append("/")
                        .append(latestBp.getDiastolicBp()).append("mmHg。");
            }
            ExerciseDaily latestGlucose = dailies.stream()
                    .filter(item -> item.getBloodGlucose() != null).findFirst().orElse(null);
            if (latestGlucose != null) {
                builder.append("最近血糖 ").append(latestGlucose.getBloodGlucose()
                        .stripTrailingZeros().toPlainString()).append("mmol/L。");
            }
        }
        return builder.toString();
    }

    private void saveResult(AiSuggestion suggestion, DeepSeekClient.ChatResult result,
                            PromptTemplateService.RenderedPrompt prompt) throws IOException {
        JsonNode root = objectMapper.readTree(AiJsonUtils.stripCodeFences(result.content()));
        String assessment = root.path("assessment").asText("");
        JsonNode weeklyPlan = root.path("weeklyPlan");
        if (!StringUtils.hasText(assessment) || !weeklyPlan.isArray() || weeklyPlan.isEmpty()) {
            throw new IOException("响应缺少 assessment 或 weeklyPlan");
        }
        for (JsonNode day : weeklyPlan) {
            if (!StringUtils.hasText(day.path("day").asText())
                    || !StringUtils.hasText(day.path("content").asText())) {
                throw new IOException("weeklyPlan 缺少 day 或 content");
            }
        }
        JsonNode rehab = root.path("rehabAdvice");
        JsonNode progress = root.path("progressMetrics");
        suggestion.setAssessmentMd(assessment);
        suggestion.setWeeklyPlanJson(objectMapper.writeValueAsString(weeklyPlan));
        suggestion.setRehabJson(objectMapper.writeValueAsString(rehab.isArray() ? rehab : objectMapper.createArrayNode()));
        suggestion.setPrecautionsMd(root.path("precautions").asText(""));
        suggestion.setProgressMetricsJson(objectMapper.writeValueAsString(progress.isArray() ? progress : objectMapper.createArrayNode()));
        String goal = root.path("goal").asText(null);
        if (StringUtils.hasText(goal)) {
            suggestion.setGoal(goal);
        }
        suggestion.setStatus("DONE");
        suggestion.setModel(deepSeekProperties.getModel());
        suggestion.setPromptVersion(prompt.version());
        suggestion.setTokenIn(result.promptTokens());
        suggestion.setTokenOut(result.completionTokens());
        suggestion.setDurationMs((int) result.durationMs());
        suggestion.setErrorMsg(null);
        suggestionMapper.updateById(suggestion);
    }

    private String goalLabel(String goal) {
        if (!StringUtils.hasText(goal)) {
            return "综合健康提升";
        }
        return switch (goal) {
            case "FAT_LOSS" -> "减脂";
            case "GLUCOSE" -> "血糖控制";
            case "BLOOD_PRESSURE" -> "血压管理";
            case "REHAB" -> "康复训练";
            case "MUSCLE" -> "增肌";
            case "MAINTAIN" -> "保持健康";
            default -> goal;
        };
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
