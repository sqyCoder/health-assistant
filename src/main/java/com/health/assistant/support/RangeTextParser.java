package com.health.assistant.support;

import com.health.assistant.entity.MetricReferenceRange;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RangeTextParser {

    private static final Pattern BETWEEN_PATTERN = Pattern.compile(
            "^(-?\\d+(?:\\.\\d+)?)\\s*[-~至]\\s*(-?\\d+(?:\\.\\d+)?)$");
    private static final Pattern MAX_PATTERN = Pattern.compile("^[<≤]\\s*(-?\\d+(?:\\.\\d+)?)$");
    private static final Pattern MIN_PATTERN = Pattern.compile("^[>≥]\\s*(-?\\d+(?:\\.\\d+)?)$");

    public MetricReferenceRange parse(String refRangeText) {
        if (!StringUtils.hasText(refRangeText)) {
            return null;
        }
        String text = refRangeText.trim();
        Matcher between = BETWEEN_PATTERN.matcher(text);
        if (between.matches()) {
            MetricReferenceRange range = new MetricReferenceRange();
            range.setRefMin(new BigDecimal(between.group(1)));
            range.setRefMax(new BigDecimal(between.group(2)));
            range.setRefText(text);
            return range;
        }
        Matcher max = MAX_PATTERN.matcher(text);
        if (max.matches()) {
            MetricReferenceRange range = new MetricReferenceRange();
            range.setRefMax(new BigDecimal(max.group(1)));
            range.setRefText(text);
            return range;
        }
        Matcher min = MIN_PATTERN.matcher(text);
        if (min.matches()) {
            MetricReferenceRange range = new MetricReferenceRange();
            range.setRefMin(new BigDecimal(min.group(1)));
            range.setRefText(text);
            return range;
        }
        if ("阴性".equals(text) || "正常".equals(text)) {
            MetricReferenceRange range = new MetricReferenceRange();
            range.setNormalText("阴性".equals(text) ? "阴性" : "正常");
            range.setRefText(text);
            return range;
        }
        return null;
    }
}
