package com.health.assistant.dto.member;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class LifestyleRequest {

    private String smoking;

    @Size(max = 50, message = "吸烟量最长 50 个字符")
    private String smokingAmount;

    private String drinking;

    @Size(max = 50, message = "饮酒量最长 50 个字符")
    private String drinkingAmount;

    @DecimalMin(value = "0", message = "睡眠时长不能为负数")
    @DecimalMax(value = "24", message = "睡眠时长不能超过 24 小时")
    private BigDecimal sleepHours;

    private String sleepQuality;

    @Size(max = 50, message = "运动频率最长 50 个字符")
    private String exerciseFrequency;

    @DecimalMin(value = "0", message = "久坐时长不能为负数")
    @DecimalMax(value = "24", message = "久坐时长不能超过 24 小时")
    private BigDecimal sedentaryHours;

    @Size(max = 255, message = "饮食习惯最长 255 个字符")
    private String dietHabit;

    private String stressLevel;
}
