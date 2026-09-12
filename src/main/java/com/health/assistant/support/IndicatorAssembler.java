package com.health.assistant.support;

import com.health.assistant.common.exception.BizException;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.entity.ExamIndicator;
import com.health.assistant.entity.ExamReport;
import com.health.assistant.entity.FamilyMember;
import com.health.assistant.entity.MetricCategory;
import com.health.assistant.entity.MetricDict;
import com.health.assistant.entity.MetricReferenceRange;
import com.health.assistant.mapper.MetricCategoryMapper;
import com.health.assistant.mapper.MetricDictMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class IndicatorAssembler {

    private final MetricDictMapper metricDictMapper;
    private final MetricCategoryMapper metricCategoryMapper;
    private final MetricMatcher metricMatcher;
    private final MetricRangeResolver rangeResolver;
    private final RangeTextParser rangeTextParser;
    private final RangeEvaluator rangeEvaluator;

    public ExamIndicator buildByMetricId(ExamReport report, FamilyMember member, Integer age, Long metricId,
                                         String valueText, String unit, String refRangeText, String source, int sort) {
        MetricDict metric = metricDictMapper.selectById(metricId);
        if (metric == null || !Integer.valueOf(1).equals(metric.getStatus())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "指标不存在: " + metricId);
        }
        ExamIndicator indicator = baseIndicator(report, source, sort, valueText, unit, refRangeText);
        applyMetric(indicator, metric, categoryNames());
        indicator.setMatchType("EXACT");
        MetricReferenceRange range = rangeResolver.resolve(metric.getId(), member.getGender(), age);
        applyRangeAndJudge(indicator, range);
        return indicator;
    }

    public ExamIndicator buildByRawName(ExamReport report, FamilyMember member, Integer age, String rawName,
                                        String valueText, String unit, String refRangeText, String categoryHint,
                                        String source, int sort) {
        ExamIndicator indicator = baseIndicator(report, source, sort, valueText, unit, refRangeText);
        indicator.setMetricName(rawName);
        MetricMatcher.MatchResult match = metricMatcher.match(rawName);
        MetricReferenceRange range = null;
        if (match != null) {
            MetricDict metric = match.metric();
            applyMetric(indicator, metric, categoryNames());
            if (!StringUtils.hasText(indicator.getUnit())) {
                indicator.setUnit(metric.getUnit());
            }
            indicator.setMatchType(match.matchType());
            range = rangeResolver.resolve(metric.getId(), member.getGender(), age);
        } else {
            indicator.setMatchType("CUSTOM");
            indicator.setCategoryName(categoryHint);
        }
        applyRangeAndJudge(indicator, range);
        return indicator;
    }

    private ExamIndicator baseIndicator(ExamReport report, String source, int sort,
                                        String valueText, String unit, String refRangeText) {
        ExamIndicator indicator = new ExamIndicator();
        indicator.setReportId(report.getId());
        indicator.setMemberId(report.getMemberId());
        indicator.setValueText(valueText);
        indicator.setUnit(unit);
        indicator.setRefRangeText(refRangeText);
        indicator.setSource(source);
        indicator.setSort(sort);
        return indicator;
    }

    private void applyMetric(ExamIndicator indicator, MetricDict metric, Map<Long, String> categoryNames) {
        indicator.setMetricId(metric.getId());
        indicator.setMetricCode(metric.getCode());
        indicator.setMetricName(metric.getName());
        indicator.setCategoryName(categoryNames.get(metric.getCategoryId()));
    }

    private void applyRangeAndJudge(ExamIndicator indicator, MetricReferenceRange range) {
        if (range == null && StringUtils.hasText(indicator.getRefRangeText())) {
            range = rangeTextParser.parse(indicator.getRefRangeText());
        }
        if (range != null) {
            indicator.setRefMin(range.getRefMin());
            indicator.setRefMax(range.getRefMax());
            if (!StringUtils.hasText(indicator.getRefRangeText())) {
                indicator.setRefRangeText(StringUtils.hasText(range.getRefText())
                        ? range.getRefText() : range.getNormalText());
            }
        }
        RangeEvaluator.JudgeResult judge = rangeEvaluator.evaluate(indicator.getValueText(), range);
        indicator.setValueNumeric(judge.valueNumeric());
        indicator.setJudgeStatus(judge.status());
        indicator.setJudgeNote(judge.note());
    }

    private Map<Long, String> categoryNames() {
        return metricCategoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(MetricCategory::getId, MetricCategory::getName, (a, b) -> a));
    }
}
