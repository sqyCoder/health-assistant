package com.health.assistant.dto.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class MedicalHistoryRequest {

    @NotBlank(message = "疾病名称不能为空")
    @Size(max = 100, message = "疾病名称最长 100 个字符")
    private String diseaseName;

    private LocalDate diagnosedDate;

    @NotBlank(message = "疾病状态不能为空")
    private String diseaseStatus;

    @Size(max = 500, message = "治疗情况最长 500 个字符")
    private String treatmentDesc;

    @Size(max = 100, message = "医院最长 100 个字符")
    private String hospital;

    @Size(max = 500, message = "备注最长 500 个字符")
    private String remark;
}
