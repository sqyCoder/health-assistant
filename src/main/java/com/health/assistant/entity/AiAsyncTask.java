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
@TableName(value = "ai_async_task", autoResultMap = true)
public class AiAsyncTask {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long memberId;

    private String taskType;

    private String bizType;

    private Long bizId;

    private String status;

    private Integer progress;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String payloadJson;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String resultJson;

    private Integer retryCount;

    private Integer maxRetry;

    private String errorMsg;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
