package com.health.assistant.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.assistant.common.exception.BizException;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.entity.AiAsyncTask;
import com.health.assistant.entity.AiInterpretation;
import com.health.assistant.entity.AiSuggestion;
import com.health.assistant.entity.ExamReport;
import com.health.assistant.entity.ExportRecord;
import com.health.assistant.entity.FamilyMember;
import com.health.assistant.entity.FileObject;
import com.health.assistant.mapper.AiInterpretationMapper;
import com.health.assistant.mapper.AiSuggestionMapper;
import com.health.assistant.mapper.ExamReportMapper;
import com.health.assistant.mapper.ExportRecordMapper;
import com.health.assistant.mapper.FamilyMemberMapper;
import com.health.assistant.mapper.FileObjectMapper;
import com.health.assistant.storage.StorageProperties;
import com.health.assistant.storage.StorageService;
import com.lowagie.text.pdf.BaseFont;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExportPdfTaskHandler implements TaskHandler {

    public static final String TASK_TYPE = "PDF_EXPORT";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ExportRecordMapper exportRecordMapper;
    private final AiInterpretationMapper interpretationMapper;
    private final AiSuggestionMapper suggestionMapper;
    private final ExamReportMapper reportMapper;
    private final FamilyMemberMapper memberMapper;
    private final FileObjectMapper fileObjectMapper;
    private final StorageService storageService;
    private final StorageProperties storageProperties;
    private final Configuration freemarkerConfiguration;
    private final ObjectMapper objectMapper;

    @Value("${app.export.font-path:C:/Windows/Fonts/simhei.ttf}")
    private String fontPath;

    @Value("${app.export.font-family:SimHei}")
    private String fontFamily;

    @Override
    public String getTaskType() {
        return TASK_TYPE;
    }

    @Override
    public String handle(AiAsyncTask task) {
        ExportRecord record = exportRecordMapper.selectById(task.getBizId());
        if (record == null) {
            throw new BizException(ErrorCode.EXPORT_FAILED, "导出记录不存在");
        }
        FamilyMember member = memberMapper.selectById(record.getMemberId());
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("member", member);
            model.put("fontFamily", fontFamily);
            model.put("generatedAt", LocalDateTime.now().format(TIME_FORMAT));
            String templateName = buildModel(record, model);
            String html = renderTemplate(templateName, model);
            byte[] pdf = renderPdf(html);

            String objectKey = "exports/" + record.getUserId() + "/" + record.getMemberId() + "/"
                    + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "/"
                    + UUID.randomUUID().toString().replace("-", "") + ".pdf";
            String bucket = storageProperties.getSupabase().getBucketExports();
            try (ByteArrayInputStream in = new ByteArrayInputStream(pdf)) {
                storageService.upload(bucket, objectKey, in, "application/pdf", pdf.length);
            }

            FileObject fileObject = new FileObject();
            fileObject.setUserId(record.getUserId());
            fileObject.setMemberId(record.getMemberId());
            fileObject.setBizType("EXPORT");
            fileObject.setOriginalName(record.getExportType().toLowerCase() + "-" + member.getName() + ".pdf");
            fileObject.setStorageType(storageProperties.getType());
            fileObject.setBucket(bucket);
            fileObject.setObjectKey(objectKey);
            fileObject.setFileSize((long) pdf.length);
            fileObject.setMimeType("application/pdf");
            fileObject.setStatus("ACTIVE");
            fileObjectMapper.insert(fileObject);

            record.setStatus("SUCCESS");
            record.setFileId(fileObject.getId());
            record.setFinishedAt(LocalDateTime.now());
            exportRecordMapper.updateById(record);
            return objectMapper.writeValueAsString(Map.of(
                    "exportId", record.getId(),
                    "fileId", fileObject.getId()));
        } catch (Exception e) {
            log.error("export pdf failed: recordId={}", record.getId(), e);
            record.setStatus("FAILED");
            record.setErrorMsg(e.getMessage() == null ? "unknown"
                    : e.getMessage().substring(0, Math.min(e.getMessage().length(), 1000)));
            record.setFinishedAt(LocalDateTime.now());
            exportRecordMapper.updateById(record);
            if (e instanceof BizException bizException) {
                throw bizException;
            }
            throw new BizException(ErrorCode.EXPORT_FAILED, "PDF 导出失败: " + e.getMessage());
        }
    }

    private String buildModel(ExportRecord record, Map<String, Object> model) throws Exception {
        if ("INTERPRETATION".equals(record.getExportType())) {
            AiInterpretation interpretation = interpretationMapper.selectById(record.getRefId());
            if (interpretation == null || interpretation.getContentJson() == null) {
                throw new BizException(ErrorCode.EXPORT_FAILED, "解读数据不存在");
            }
            ExamReport report = reportMapper.selectById(interpretation.getReportId());
            JsonNode root = objectMapper.readTree(interpretation.getContentJson());
            List<Map<String, String>> cards = new ArrayList<>();
            for (JsonNode node : root.path("indicators")) {
                Map<String, String> card = new LinkedHashMap<>();
                card.put("name", node.path("name").asText());
                card.put("value", node.path("value").asText());
                card.put("unit", node.path("unit").asText(""));
                card.put("refRange", node.path("refRange").asText("-"));
                card.put("status", node.path("status").asText("UNKNOWN"));
                card.put("statusLabel", judgeLabel(node.path("status").asText("UNKNOWN")));
                card.put("plainExplain", node.path("plainExplain").asText(""));
                card.put("advice", node.path("advice").asText(""));
                cards.add(card);
            }
            List<String> focusMetrics = new ArrayList<>();
            JsonNode focusNode = objectMapper.readTree(
                    interpretation.getFocusMetrics() == null ? "[]" : interpretation.getFocusMetrics());
            focusNode.forEach(node -> focusMetrics.add(node.asText()));

            model.put("report", report);
            model.put("interpretation", interpretation);
            model.put("riskLevel", interpretation.getRiskLevel() == null ? "MEDIUM" : interpretation.getRiskLevel());
            model.put("riskLabel", riskLabel(interpretation.getRiskLevel()));
            model.put("summary", root.path("summary").asText(""));
            model.put("focusMetrics", focusMetrics);
            model.put("cards", cards);
            model.put("disclaimer", root.path("disclaimer").asText(
                    "以上内容基于体检数据由 AI 生成，仅供参考，不构成医疗诊断。如有异常请遵医嘱复查就医。"));
            return "pdf/interpretation.ftl";
        }
        if ("SUGGESTION".equals(record.getExportType())) {
            AiSuggestion suggestion = suggestionMapper.selectById(record.getRefId());
            if (suggestion == null || suggestion.getWeeklyPlanJson() == null) {
                throw new BizException(ErrorCode.EXPORT_FAILED, "建议数据不存在");
            }
            List<Map<String, String>> weeklyPlan = new ArrayList<>();
            for (JsonNode node : objectMapper.readTree(suggestion.getWeeklyPlanJson())) {
                Map<String, String> day = new LinkedHashMap<>();
                day.put("day", dayLabel(node.path("day").asText()));
                day.put("title", node.path("title").asText(""));
                day.put("workoutType", workoutTypeLabel(node.path("workoutType").asText()));
                day.put("durationMin", node.path("durationMin").asText("-"));
                day.put("intensity", intensityLabel(node.path("intensity").asText()));
                day.put("content", node.path("content").asText());
                day.put("notes", node.path("notes").asText(""));
                weeklyPlan.add(day);
            }
            List<Map<String, String>> rehabList = new ArrayList<>();
            for (JsonNode node : objectMapper.readTree(suggestion.getRehabJson() == null ? "[]" : suggestion.getRehabJson())) {
                Map<String, String> rehab = new LinkedHashMap<>();
                rehab.put("target", node.path("target").asText(""));
                rehab.put("action", node.path("action").asText(""));
                rehab.put("frequency", node.path("frequency").asText(""));
                rehab.put("caution", node.path("caution").asText(""));
                rehabList.add(rehab);
            }
            List<String> progressMetrics = new ArrayList<>();
            for (JsonNode node : objectMapper.readTree(suggestion.getProgressMetricsJson() == null
                    ? "[]" : suggestion.getProgressMetricsJson())) {
                progressMetrics.add(node.asText());
            }
            model.put("assessment", suggestion.getAssessmentMd());
            model.put("goalLabel", goalLabel(suggestion.getGoal()));
            model.put("weeklyPlan", weeklyPlan);
            model.put("rehabList", rehabList);
            model.put("precautions", suggestion.getPrecautionsMd());
            model.put("progressMetrics", progressMetrics);
            model.put("disclaimer", "以上建议由 AI 生成，仅供参考，不构成医疗建议。如有不适请及时就医。");
            return "pdf/suggestion.ftl";
        }
        throw new BizException(ErrorCode.PARAM_ERROR, "不支持的导出类型: " + record.getExportType());
    }

    private String renderTemplate(String templateName, Map<String, Object> model) throws Exception {
        Template template = freemarkerConfiguration.getTemplate(templateName, "UTF-8");
        StringWriter writer = new StringWriter();
        template.process(model, writer);
        return writer.toString();
    }

    private byte[] renderPdf(String html) throws Exception {
        File fontFile = new File(fontPath);
        if (!fontFile.exists()) {
            throw new BizException(ErrorCode.EXPORT_FAILED,
                    "中文字体文件不存在: " + fontPath + "，请配置 app.export.font-path");
        }
        ITextRenderer renderer = new ITextRenderer();
        renderer.getFontResolver().addFont(fontFile.getAbsolutePath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        renderer.setDocumentFromString(html);
        renderer.layout();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        renderer.createPDF(output);
        return output.toByteArray();
    }

    private String judgeLabel(String status) {
        return switch (status) {
            case "NORMAL" -> "正常";
            case "HIGH" -> "偏高";
            case "LOW" -> "偏低";
            case "ABNORMAL" -> "异常";
            case "POSITIVE" -> "阳性";
            case "NEGATIVE" -> "阴性";
            default -> "未知";
        };
    }

    private String riskLabel(String riskLevel) {
        return switch (riskLevel == null ? "" : riskLevel) {
            case "LOW" -> "低风险";
            case "HIGH" -> "高风险";
            default -> "中风险";
        };
    }

    private String dayLabel(String day) {
        return switch (day == null ? "" : day) {
            case "MONDAY" -> "周一";
            case "TUESDAY" -> "周二";
            case "WEDNESDAY" -> "周三";
            case "THURSDAY" -> "周四";
            case "FRIDAY" -> "周五";
            case "SATURDAY" -> "周六";
            case "SUNDAY" -> "周日";
            default -> day;
        };
    }

    private String workoutTypeLabel(String type) {
        return switch (type == null ? "" : type) {
            case "RUNNING" -> "跑步";
            case "WALKING" -> "步行";
            case "CYCLING" -> "骑行";
            case "SWIMMING" -> "游泳";
            case "STRENGTH" -> "力量训练";
            case "YOGA" -> "瑜伽";
            case "BALL_GAME" -> "球类";
            case "HIKING" -> "登山";
            default -> "其他";
        };
    }

    private String intensityLabel(String intensity) {
        return switch (intensity == null ? "" : intensity) {
            case "LOW" -> "低强度";
            case "HIGH" -> "高强度";
            default -> "中强度";
        };
    }

    private String goalLabel(String goal) {
        return switch (goal == null ? "" : goal) {
            case "FAT_LOSS" -> "减脂";
            case "GLUCOSE" -> "血糖控制";
            case "BLOOD_PRESSURE" -> "血压管理";
            case "REHAB" -> "康复训练";
            case "MUSCLE" -> "增肌";
            default -> "保持健康";
        };
    }
}
