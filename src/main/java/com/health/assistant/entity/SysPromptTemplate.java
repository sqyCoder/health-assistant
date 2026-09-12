package com.health.assistant.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.health.assistant.config.mybatis.JsonbTypeHandler;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName(value = "sys_prompt_template", autoResultMap = true)
public class SysPromptTemplate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String code;

    private String name;

    private String scene;

    private Integer version;

    private String content;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String variablesJson;

    private BigDecimal temperature;

    private Integer maxTokens;

    private String status;

    private Integer isDefault;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
