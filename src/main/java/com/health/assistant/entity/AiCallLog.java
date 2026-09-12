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
@TableName("ai_call_log")
public class AiCallLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long taskId;

    private String scene;

    private String model;

    private String requestId;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private java.math.BigDecimal costEstimate;

    private Integer durationMs;

    private Integer isStream;

    private String status;

    private String errorCode;

    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
