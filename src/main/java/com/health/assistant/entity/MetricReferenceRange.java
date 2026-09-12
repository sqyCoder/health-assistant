package com.health.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@TableName("metric_reference_range")
public class MetricReferenceRange {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long metricId;

    private String gender;

    private Integer ageMin;

    private Integer ageMax;

    private BigDecimal refMin;

    private BigDecimal refMax;

    private String refText;

    private String normalText;

    private Integer status;
}
