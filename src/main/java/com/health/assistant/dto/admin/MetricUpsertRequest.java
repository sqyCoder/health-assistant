package com.health.assistant.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MetricUpsertRequest {

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotBlank(message = "指标编码不能为空")
    private String code;

    @NotBlank(message = "指标名称不能为空")
    private String name;

    private String nameEn;

    private String alias;

    private String valueType = "NUMERIC";

    private String unit;

    private String description;

    private String highDesc;

    private String lowDesc;

    private Integer sort;

    private Integer status = 1;
}
