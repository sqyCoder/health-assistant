package com.health.assistant.dto.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AllergyRequest {

    @NotBlank(message = "过敏原不能为空")
    @Size(max = 100, message = "过敏原最长 100 个字符")
    private String allergen;

    @NotBlank(message = "过敏类型不能为空")
    private String allergyType;

    @Size(max = 200, message = "过敏反应最长 200 个字符")
    private String reaction;

    private String severity;

    @Size(max = 500, message = "备注最长 500 个字符")
    private String remark;
}
