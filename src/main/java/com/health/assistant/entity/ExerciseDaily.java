package com.health.assistant.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("exercise_daily")
public class ExerciseDaily {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long memberId;

    private Long userId;

    private LocalDate recordDate;

    private Integer steps;

    private Integer activeMinutes;

    private BigDecimal caloriesKcal;

    private BigDecimal distanceKm;

    private BigDecimal sleepHours;

    private String sleepQuality;

    private Integer restingHeartRate;

    private BigDecimal weightKg;

    private Integer systolicBp;

    private Integer diastolicBp;

    private BigDecimal bloodGlucose;

    private String glucoseType;

    private String mood;

    private String note;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
