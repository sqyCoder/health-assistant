package com.health.assistant.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("metric_dict")
public class MetricDict {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long categoryId;

    private String code;

    private String name;

    private String nameEn;

    private String alias;

    private String valueType;

    private String unit;

    private String description;

    private String highDesc;

    private String lowDesc;

    private Integer sort;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
