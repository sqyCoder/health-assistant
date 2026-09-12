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
@TableName(value = "sys_audit_log", autoResultMap = true)
public class SysAuditLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String username;

    private String action;

    private String resourceType;

    private Long resourceId;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String detailJson;

    private String ip;

    private String userAgent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
