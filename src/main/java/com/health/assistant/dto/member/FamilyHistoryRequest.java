package com.health.assistant.dto.member;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FamilyHistoryRequest {

    @NotBlank(message = "亲属关系不能为空")
    private String relation;

    @NotBlank(message = "疾病名称不能为空")
    @Size(max = 100, message = "疾病名称最长 100 个字符")
    private String diseaseName;

    @Min(value = 0, message = "发病年龄不能为负数")
    @Max(value = 150, message = "发病年龄数值过大")
    private Integer onsetAge;

    @Size(max = 500, message = "备注最长 500 个字符")
    private String remark;
}
