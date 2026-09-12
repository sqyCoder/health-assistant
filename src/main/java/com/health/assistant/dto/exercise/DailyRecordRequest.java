package com.health.assistant.dto.exercise;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class DailyRecordRequest {

    @NotNull(message = "记录日期不能为空")
    private LocalDate recordDate;

    @Min(value = 0, message = "步数不能为负")
    @Max(value = 200000, message = "步数数值过大")
    private Integer steps;

    @Min(value = 0, message = "活动时长不能为负")
    @Max(value = 1440, message = "活动时长不能超过 1440 分钟")
    private Integer activeMinutes;

    @DecimalMin(value = "0", message = "消耗不能为负")
    @DecimalMax(value = "10000", message = "消耗数值过大")
    private BigDecimal caloriesKcal;

    @DecimalMin(value = "0", message = "距离不能为负")
    @DecimalMax(value = "200", message = "距离数值过大")
    private BigDecimal distanceKm;

    @DecimalMin(value = "0", message = "睡眠时长不能为负")
    @DecimalMax(value = "24", message = "睡眠时长不能超过 24 小时")
    private BigDecimal sleepHours;

    private String sleepQuality;

    @Min(value = 20, message = "静息心率数值过小")
    @Max(value = 250, message = "静息心率数值过大")
    private Integer restingHeartRate;

    @DecimalMin(value = "0", message = "体重不能为负")
    @DecimalMax(value = "500", message = "体重数值过大")
    private BigDecimal weightKg;

    @Min(value = 40, message = "收缩压数值过小")
    @Max(value = 300, message = "收缩压数值过大")
    private Integer systolicBp;

    @Min(value = 20, message = "舒张压数值过小")
    @Max(value = 200, message = "舒张压数值过大")
    private Integer diastolicBp;

    @DecimalMin(value = "0", message = "血糖不能为负")
    @DecimalMax(value = "50", message = "血糖数值过大")
    private BigDecimal bloodGlucose;

    private String glucoseType;

    private String mood;

    @Size(max = 500, message = "备注最长 500 个字符")
    private String note;
}
