package com.health.assistant.dto.member;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class MemberSaveRequest {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名最长 50 个字符")
    private String name;

    @NotBlank(message = "与账号主人的关系不能为空")
    private String relation;

    @NotNull(message = "性别不能为空")
    @Min(value = 1, message = "性别取值非法")
    @Max(value = 2, message = "性别取值非法")
    private Integer gender;

    @PastOrPresent(message = "出生日期不能晚于今天")
    private LocalDate birthDate;

    @DecimalMin(value = "0", message = "身高不能为负数")
    @DecimalMax(value = "300", message = "身高数值过大")
    private BigDecimal heightCm;

    @DecimalMin(value = "0", message = "体重不能为负数")
    @DecimalMax(value = "500", message = "体重数值过大")
    private BigDecimal weightKg;

    @Size(max = 10, message = "血型最长 10 个字符")
    private String bloodType;

    @Size(max = 30, message = "电话最长 30 个字符")
    private String phone;

    @Size(max = 500, message = "头像地址最长 500 个字符")
    private String avatarUrl;

    @Size(max = 500, message = "备注最长 500 个字符")
    private String remark;

    private Integer sort;
}
