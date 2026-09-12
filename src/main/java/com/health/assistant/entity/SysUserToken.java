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
@TableName("sys_user_token")
public class SysUserToken {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String tokenHash;

    private LocalDateTime expiresAt;

    private Integer revoked;

    private String clientInfo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
