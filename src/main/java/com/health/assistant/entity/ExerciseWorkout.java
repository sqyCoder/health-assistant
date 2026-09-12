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
import java.time.LocalTime;

@Getter
@Setter
@TableName("exercise_workout")
public class ExerciseWorkout {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long memberId;

    private Long userId;

    private LocalDate workoutDate;

    private LocalTime startTime;

    private String workoutType;

    private String title;

    private Integer durationMin;

    private String intensity;

    private BigDecimal distanceKm;

    private Integer avgHeartRate;

    private Integer maxHeartRate;

    private Integer paceSecPerKm;

    private BigDecimal caloriesKcal;

    private String feeling;

    private String note;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
