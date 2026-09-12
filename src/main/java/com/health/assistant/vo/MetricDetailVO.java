package com.health.assistant.vo;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MetricDetailVO {

    private final Long id;
    private final String code;
    private final String name;
    private final String nameEn;
    private final String alias;
    private final String valueType;
    private final String unit;
    private final String categoryName;
    private final String description;
    private final String highDesc;
    private final String lowDesc;
    private final List<MetricRangeVO> ranges;
}
