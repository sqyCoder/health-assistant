package com.health.assistant.vo;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReportDetailVO {

    private final ReportVO report;
    private final List<IndicatorVO> indicators;
}
