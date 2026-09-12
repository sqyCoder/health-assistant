package com.health.assistant.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PromptUpsertRequest {

    @NotBlank(message = "模板编码不能为空")
    private String code;

    @NotBlank(message = "模板名称不能为空")
    private String name;

    private String scene;

    @NotBlank(message = "模板内容不能为空")
    private String content;

    private BigDecimal temperature = new BigDecimal("0.30");

    private Integer maxTokens = 4096;
}
