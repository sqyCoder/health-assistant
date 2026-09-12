package com.health.assistant.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("member_lifestyle")
public class MemberLifestyle {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long memberId;

    private String smoking;

    private String smokingAmount;

    private String drinking;

    private String drinkingAmount;

    private BigDecimal sleepHours;

    private String sleepQuality;

    private String exerciseFrequency;

    private BigDecimal sedentaryHours;

    private String dietHabit;

    private String stressLevel;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
