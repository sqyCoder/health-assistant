package com.health.assistant.support;

import com.health.assistant.common.constant.ReportConstants;
import com.health.assistant.entity.MetricReferenceRange;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RangeEvaluator {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");

    public JudgeResult evaluate(String valueText, MetricReferenceRange range) {
        BigDecimal numeric = parseNumeric(valueText);
        if (range == null) {
            return new JudgeResult(numeric, ReportConstants.JUDGE_UNKNOWN, "无参考范围");
        }
        boolean hasNumericRange = range.getRefMin() != null || range.getRefMax() != null;
        if (numeric != null && hasNumericRange) {
            if (range.getRefMin() != null && numeric.compareTo(range.getRefMin()) < 0) {
                return new JudgeResult(numeric, ReportConstants.JUDGE_LOW,
                        "低于参考下限 " + describeRange(range));
            }
            if (range.getRefMax() != null && numeric.compareTo(range.getRefMax()) > 0) {
                return new JudgeResult(numeric, ReportConstants.JUDGE_HIGH,
                        "高于参考上限 " + describeRange(range));
            }
            return new JudgeResult(numeric, ReportConstants.JUDGE_NORMAL, null);
        }
        return evaluateQualitative(valueText, range, numeric);
    }

    private JudgeResult evaluateQualitative(String valueText, MetricReferenceRange range, BigDecimal numeric) {
        if (!StringUtils.hasText(valueText)) {
            return new JudgeResult(numeric, ReportConstants.JUDGE_UNKNOWN, "无检验值");
        }
        String normalText = range.getNormalText();
        if (StringUtils.hasText(normalText)) {
            if (valueText.contains(normalText)) {
                return new JudgeResult(numeric, ReportConstants.JUDGE_NEGATIVE, null);
            }
            if (valueText.contains("阳") || valueText.contains("+")) {
                return new JudgeResult(numeric, ReportConstants.JUDGE_POSITIVE, "阳性结果，建议结合临床复查");
            }
            return new JudgeResult(numeric, ReportConstants.JUDGE_ABNORMAL, "结果异常");
        }
        if (StringUtils.hasText(range.getRefText()) && valueText.trim().equals(range.getRefText().trim())) {
            return new JudgeResult(numeric, ReportConstants.JUDGE_NORMAL, null);
        }
        return new JudgeResult(numeric, ReportConstants.JUDGE_UNKNOWN, "无参考范围");
    }

    public BigDecimal parseNumeric(String valueText) {
        if (!StringUtils.hasText(valueText)) {
            return null;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(valueText.replace(",", ""));
        if (matcher.find()) {
            try {
                return new BigDecimal(matcher.group());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private String describeRange(MetricReferenceRange range) {
        if (StringUtils.hasText(range.getRefText())) {
            return "(" + range.getRefText() + ")";
        }
        return "[" + range.getRefMin() + " ~ " + range.getRefMax() + "]";
    }

    public record JudgeResult(BigDecimal valueNumeric, String status, String note) {
    }
}
