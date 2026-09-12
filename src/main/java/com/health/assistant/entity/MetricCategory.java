package com.health.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("metric_category")
public class MetricCategory {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String code;

    private String name;

    private Integer sort;

    private Integer status;
}
