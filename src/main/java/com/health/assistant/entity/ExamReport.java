package com.health.assistant.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("exam_report")
public class ExamReport {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long memberId;

    private Long userId;

    private String reportName;

    private LocalDate reportDate;

    private String institution;

    private String reportType;

    private String sourceType;

    private Long fileId;

    private String parseStatus;

    private Integer indicatorCount;

    private Integer abnormalCount;

    private String aiStatus;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
