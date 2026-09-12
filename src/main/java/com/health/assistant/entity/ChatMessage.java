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
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long sessionId;

    private Long memberId;

    private Long userId;

    private String role;

    private String content;

    private String status;

    private String model;

    private Integer tokenIn;

    private Integer tokenOut;

    private Integer durationMs;

    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
