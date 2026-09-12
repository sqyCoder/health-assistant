package com.health.assistant.dto.exercise;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class WorkoutRequest {

    @NotNull(message = "运动日期不能为空")
    private LocalDate workoutDate;

    private LocalTime startTime;

    @NotBlank(message = "运动类型不能为空")
    private String workoutType;

    @Size(max = 100, message = "标题最长 100 个字符")
    private String title;

    @Min(value = 0, message = "时长不能为负")
    @Max(value = 1440, message = "时长不能超过 1440 分钟")
    private Integer durationMin;

    private String intensity;

    @DecimalMin(value = "0", message = "距离不能为负")
    @DecimalMax(value = "500", message = "距离数值过大")
    private BigDecimal distanceKm;

    @Min(value = 20, message = "心率数值过小")
    @Max(value = 250, message = "心率数值过大")
    private Integer avgHeartRate;

    @Min(value = 20, message = "心率数值过小")
    @Max(value = 250, message = "心率数值过大")
    private Integer maxHeartRate;

    @Min(value = 0, message = "配速不能为负")
    private Integer paceSecPerKm;

    @DecimalMin(value = "0", message = "消耗不能为负")
    @DecimalMax(value = "10000", message = "消耗数值过大")
    private BigDecimal caloriesKcal;

    private String feeling;

    @Size(max = 500, message = "备注最长 500 个字符")
    private String note;
}
