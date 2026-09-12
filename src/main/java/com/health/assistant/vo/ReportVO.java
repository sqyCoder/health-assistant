package com.health.assistant.vo;

import com.health.assistant.entity.ExamReport;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class ReportVO {

    private final Long id;
    private final Long memberId;
    private final String reportName;
    private final LocalDate reportDate;
    private final String institution;
    private final String reportType;
    private final String sourceType;
    private final Long fileId;
    private final String parseStatus;
    private final Integer indicatorCount;
    private final Integer abnormalCount;
    private final String aiStatus;
    private final String remark;
    private final LocalDateTime createdAt;

    public static ReportVO from(ExamReport entity) {
        return ReportVO.builder()
                .id(entity.getId())
                .memberId(entity.getMemberId())
                .reportName(entity.getReportName())
                .reportDate(entity.getReportDate())
                .institution(entity.getInstitution())
                .reportType(entity.getReportType())
                .sourceType(entity.getSourceType())
                .fileId(entity.getFileId())
                .parseStatus(entity.getParseStatus())
                .indicatorCount(entity.getIndicatorCount())
                .abnormalCount(entity.getAbnormalCount())
                .aiStatus(entity.getAiStatus())
                .remark(entity.getRemark())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
