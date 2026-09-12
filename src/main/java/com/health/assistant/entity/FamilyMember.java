package com.health.assistant.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("family_member")
public class FamilyMember {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String name;

    private String relation;

    private Integer gender;

    private LocalDate birthDate;

    private BigDecimal heightCm;

    private BigDecimal weightKg;

    private String bloodType;

    private String phone;

    private String idCardCipher;

    private String avatarUrl;

    private String remark;

    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
