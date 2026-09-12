package com.health.assistant.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.health.assistant.config.mybatis.JsonbTypeHandler;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName(value = "ai_interpretation", autoResultMap = true)
public class AiInterpretation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long reportId;

    private Long memberId;

    private Long userId;

    private String status;

    private String riskLevel;

    private String summary;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String contentJson;

    private String contentMd;

    private Integer abnormalCount;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String focusMetrics;

    private String model;

    private Integer promptVersion;

    private Integer tokenIn;

    private Integer tokenOut;

    private Integer durationMs;

    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
