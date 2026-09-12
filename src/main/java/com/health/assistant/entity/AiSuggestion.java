package com.health.assistant.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.health.assistant.config.mybatis.JsonbTypeHandler;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName(value = "ai_suggestion", autoResultMap = true)
public class AiSuggestion {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long memberId;

    private Long userId;

    private String suggestionType;

    private String sourceType;

    private Long sourceId;

    private String goal;

    private String status;

    private String assessmentMd;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String weeklyPlanJson;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String rehabJson;

    private String precautionsMd;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String progressMetricsJson;

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

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
