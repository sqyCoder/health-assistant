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
@TableName("exam_indicator")
public class ExamIndicator {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long reportId;

    private Long memberId;

    private Long metricId;

    private String metricCode;

    private String metricName;

    private String categoryName;

    private String valueText;

    private BigDecimal valueNumeric;

    private String unit;

    private String refRangeText;

    private BigDecimal refMin;

    private BigDecimal refMax;

    private String judgeStatus;

    private String judgeNote;

    private String matchType;

    private String source;

    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
