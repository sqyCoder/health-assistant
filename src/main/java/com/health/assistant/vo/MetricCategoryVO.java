package com.health.assistant.vo;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MetricCategoryVO {

    private final Long id;
    private final String code;
    private final String name;
    private final Integer sort;
}
