package com.health.assistant.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.assistant.client.DeepSeekClient;
import com.health.assistant.common.constant.ReportConstants;
import com.health.assistant.common.exception.BizException;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.entity.AiAsyncTask;
import com.health.assistant.entity.ExamIndicator;
import com.health.assistant.entity.ExamReport;
import com.health.assistant.entity.FamilyMember;
import com.health.assistant.entity.FileObject;
import com.health.assistant.mapper.ExamIndicatorMapper;
import com.health.assistant.mapper.ExamReportMapper;
import com.health.assistant.mapper.FamilyMemberMapper;
import com.health.assistant.mapper.FileObjectMapper;
import com.health.assistant.service.FileService;
import com.health.assistant.service.impl.PromptTemplateService;
import com.health.assistant.support.AiJsonUtils;
import com.health.assistant.support.AiLogRecorder;
import com.health.assistant.support.IndicatorAssembler;
import com.health.assistant.support.PdfTextExtractor;
import com.health.assistant.support.ReportTextCleaner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportParseTaskHandler implements TaskHandler {

    public static final String TASK_TYPE = "REPORT_PARSE";
    private static final int MAX_TEXT_LENGTH = 15000;

    private final ExamReportMapper reportMapper;
    private final ExamIndicatorMapper indicatorMapper;
    private final FileObjectMapper fileObjectMapper;
    private final FamilyMemberMapper memberMapper;
    private final FileService fileService;
    private final PdfTextExtractor pdfTextExtractor;
    private final ReportTextCleaner reportTextCleaner;
    private final IndicatorAssembler indicatorAssembler;
    private final PromptTemplateService promptTemplateService;
    private final DeepSeekClient deepSeekClient;
    private final AiLogRecorder aiLogRecorder;
    private final ObjectMapper objectMapper;

    @Override
    public String getTaskType() {
        return TASK_TYPE;
    }

    @Override
    public String handle(AiAsyncTask task) {
        ExamReport report = reportMapper.selectById(task.getBizId());
        if (report == null) {
            throw new BizException(ErrorCode.REPORT_NOT_FOUND);
        }
        report.setParseStatus(ReportConstants.PARSE_PARSING);
        reportMapper.updateById(report);
        try {
            PdfTextExtractor.Extraction extraction = pdfTextExtractor.extract(loadPdf(report));
            if (extraction.scanned()) {
                report.setParseStatus(ReportConstants.PARSE_NEED_MANUAL);
                reportMapper.updateById(report);
                return objectMapper.writeValueAsString(Map.of(
                        "scanned", true,
                        "message", "检测到扫描版 PDF，请手动录入指标"));
            }
            String text = reportTextCleaner.clean(extraction.text());
            if (text.length() > MAX_TEXT_LENGTH) {
                text = text.substring(0, MAX_TEXT_LENGTH);
            }
            List<ParsedIndicator> parsed = parseWithAi(task, text);
            int abnormal = saveIndicators(report, parsed);
            report.setIndicatorCount(parsed.size());
            report.setAbnormalCount(abnormal);
            report.setParseStatus(ReportConstants.PARSE_PARSED);
            reportMapper.updateById(report);
            return objectMapper.writeValueAsString(Map.of(
                    "scanned", false,
                    "indicatorCount", parsed.size(),
                    "abnormalCount", abnormal));
        } catch (BizException e) {
            report.setParseStatus(ReportConstants.PARSE_FAILED);
            reportMapper.updateById(report);
            throw e;
        } catch (Exception e) {
            report.setParseStatus(ReportConstants.PARSE_FAILED);
            reportMapper.updateById(report);
            throw new BizException(ErrorCode.PDF_PARSE_FAILED, "解析失败: " + e.getMessage());
        }
    }

    private byte[] loadPdf(ExamReport report) {
        if (report.getFileId() == null) {
            throw new BizException(ErrorCode.PDF_PARSE_FAILED, "报告缺少 PDF 文件");
        }
        FileObject fileObject = fileObjectMapper.selectById(report.getFileId());
        if (fileObject == null) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }
        try (InputStream in = fileService.openStream(fileObject.getBucket(), fileObject.getObjectKey())) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new BizException(ErrorCode.PDF_PARSE_FAILED, "读取 PDF 失败: " + e.getMessage());
        }
    }

    private List<ParsedIndicator> parseWithAi(AiAsyncTask task, String reportText) {
        PromptTemplateService.RenderedPrompt prompt = promptTemplateService.render("REPORT_PARSE", Map.of());
        DeepSeekClient.ChatResult result = deepSeekClient.chat(new DeepSeekClient.ChatRequest(
                prompt.content(), reportText, prompt.temperature(), prompt.maxTokens(), true));
        aiLogRecorder.recordSuccess(task.getUserId(), task.getId(), "REPORT_PARSE", result);
        try {
            return parseJson(result.content());
        } catch (IOException | RuntimeException first) {
            log.warn("AI parse output invalid, retrying with repair prompt: {}", first.getMessage());
            String repairText = reportText
                    + "\n\n注意：你上次的输出不是合法 JSON。请严格只输出 JSON 对象，不要任何解释或 markdown 代码块。";
            DeepSeekClient.ChatResult retry = deepSeekClient.chat(new DeepSeekClient.ChatRequest(
                    prompt.content(), repairText, prompt.temperature(), prompt.maxTokens(), true));
            aiLogRecorder.recordSuccess(task.getUserId(), task.getId(), "REPORT_PARSE", retry);
            try {
                return parseJson(retry.content());
            } catch (IOException e) {
                throw new BizException(ErrorCode.AI_FORMAT_ERROR, "AI 返回格式异常: " + e.getMessage());
            }
        }
    }

    private List<ParsedIndicator> parseJson(String content) throws IOException {
        JsonNode root = objectMapper.readTree(AiJsonUtils.stripCodeFences(content));
        JsonNode array = root.path("indicators");
        if (!array.isArray() || array.isEmpty()) {
            throw new IOException("响应缺少 indicators 数组");
        }
        List<ParsedIndicator> result = new ArrayList<>();
        for (JsonNode node : array) {
            String name = textOrNull(node, "name");
            if (!StringUtils.hasText(name)) {
                continue;
            }
            result.add(new ParsedIndicator(
                    name,
                    textOrNull(node, "value"),
                    textOrNull(node, "unit"),
                    textOrNull(node, "refRange"),
                    textOrNull(node, "category")));
        }
        if (result.isEmpty()) {
            throw new IOException("未解析出任何指标");
        }
        return result;
    }

    private int saveIndicators(ExamReport report, List<ParsedIndicator> parsed) {
        FamilyMember member = memberMapper.selectById(report.getMemberId());
        if (member == null) {
            throw new BizException(ErrorCode.MEMBER_NOT_FOUND);
        }
        Integer age = member.getBirthDate() == null
                ? null
                : Period.between(member.getBirthDate(), LocalDate.now()).getYears();

        indicatorMapper.delete(Wrappers.<ExamIndicator>lambdaQuery()
                .eq(ExamIndicator::getReportId, report.getId()));
        int abnormal = 0;
        int sort = 0;
        for (ParsedIndicator item : parsed) {
            ExamIndicator indicator = indicatorAssembler.buildByRawName(report, member, age,
                    item.name(), item.value(), item.unit(), item.refRange(), item.category(), "AI", ++sort);
            indicatorMapper.insert(indicator);
            if (ReportConstants.ABNORMAL_JUDGE_STATUSES.contains(indicator.getJudgeStatus())) {
                abnormal++;
            }
        }
        return abnormal;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private record ParsedIndicator(String name, String value, String unit, String refRange, String category) {
    }
}
