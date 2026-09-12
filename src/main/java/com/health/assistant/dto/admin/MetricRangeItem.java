package com.health.assistant.dto.admin;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MetricRangeItem {

    private String gender = "ALL";

    private Integer ageMin = 0;

    private Integer ageMax = 150;

    private BigDecimal refMin;

    private BigDecimal refMax;

    private String refText;

    private String normalText;
}
