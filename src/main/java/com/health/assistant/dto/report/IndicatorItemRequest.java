package com.health.assistant.dto.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IndicatorItemRequest {

    private Long metricId;

    @NotBlank(message = "指标名称不能为空")
    @Size(max = 100, message = "指标名称最长 100 个字符")
    private String metricName;

    @Size(max = 100, message = "指标值最长 100 个字符")
    private String valueText;

    @Size(max = 30, message = "单位最长 30 个字符")
    private String unit;

    @Size(max = 100, message = "参考范围最长 100 个字符")
    private String refRangeText;

    private Integer sort;
}
