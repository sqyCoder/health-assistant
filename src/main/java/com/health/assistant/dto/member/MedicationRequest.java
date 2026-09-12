package com.health.assistant.dto.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class MedicationRequest {

    @NotBlank(message = "药品名称不能为空")
    @Size(max = 100, message = "药品名称最长 100 个字符")
    private String drugName;

    @Size(max = 50, message = "剂量最长 50 个字符")
    private String dose;

    @Size(max = 50, message = "频次最长 50 个字符")
    private String frequency;

    private String route;

    private LocalDate startDate;

    private LocalDate endDate;

    @Size(max = 100, message = "用途最长 100 个字符")
    private String purpose;

    private String status;

    @Size(max = 500, message = "医嘱最长 500 个字符")
    private String doctorAdvice;

    @Size(max = 500, message = "备注最长 500 个字符")
    private String remark;
}
