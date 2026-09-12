package com.health.assistant.vo;

import com.health.assistant.entity.ExamIndicator;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class IndicatorVO {

    private final Long id;
    private final Long metricId;
    private final String metricCode;
    private final String metricName;
    private final String categoryName;
    private final String valueText;
    private final BigDecimal valueNumeric;
    private final String unit;
    private final String refRangeText;
    private final BigDecimal refMin;
    private final BigDecimal refMax;
    private final String judgeStatus;
    private final String judgeNote;
    private final String matchType;
    private final String source;
    private final Integer sort;

    public static IndicatorVO from(ExamIndicator entity) {
        return IndicatorVO.builder()
                .id(entity.getId())
                .metricId(entity.getMetricId())
                .metricCode(entity.getMetricCode())
                .metricName(entity.getMetricName())
                .categoryName(entity.getCategoryName())
                .valueText(entity.getValueText())
                .valueNumeric(entity.getValueNumeric())
                .unit(entity.getUnit())
                .refRangeText(entity.getRefRangeText())
                .refMin(entity.getRefMin())
                .refMax(entity.getRefMax())
                .judgeStatus(entity.getJudgeStatus())
                .judgeNote(entity.getJudgeNote())
                .matchType(entity.getMatchType())
                .source(entity.getSource())
                .sort(entity.getSort())
                .build();
    }
}
