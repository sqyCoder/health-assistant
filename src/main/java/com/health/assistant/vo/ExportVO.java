package com.health.assistant.vo;

import com.health.assistant.entity.ExportRecord;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ExportVO {

    private final Long id;
    private final String exportType;
    private final Long refId;
    private final Long fileId;
    private final String status;
    private final String errorMsg;
    private final LocalDateTime createdAt;
    private final LocalDateTime finishedAt;

    public static ExportVO from(ExportRecord entity) {
        return ExportVO.builder()
                .id(entity.getId())
                .exportType(entity.getExportType())
                .refId(entity.getRefId())
                .fileId(entity.getFileId())
                .status(entity.getStatus())
                .errorMsg(entity.getErrorMsg())
                .createdAt(entity.getCreatedAt())
                .finishedAt(entity.getFinishedAt())
                .build();
    }
}
