package com.health.assistant.vo;

import com.health.assistant.entity.MetricReferenceRange;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class MetricRangeVO {

    private final String gender;
    private final Integer ageMin;
    private final Integer ageMax;
    private final BigDecimal refMin;
    private final BigDecimal refMax;
    private final String refText;
    private final String normalText;

    public static MetricRangeVO from(MetricReferenceRange entity) {
        return MetricRangeVO.builder()
                .gender(entity.getGender())
                .ageMin(entity.getAgeMin())
                .ageMax(entity.getAgeMax())
                .refMin(entity.getRefMin())
                .refMax(entity.getRefMax())
                .refText(entity.getRefText())
                .normalText(entity.getNormalText())
                .build();
    }
}
