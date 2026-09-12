package com.health.assistant.vo;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MetricVO {

    private final Long id;
    private final String code;
    private final String name;
    private final String nameEn;
    private final String alias;
    private final String valueType;
    private final String unit;
    private final String categoryName;
    private final String description;
}
