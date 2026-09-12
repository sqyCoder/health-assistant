package com.health.assistant.vo;

import com.health.assistant.entity.AiInterpretation;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InterpretationVO {

    private final Long id;
    private final Long reportId;
    private final String status;
    private final String riskLevel;
    private final String summary;
    private final String contentJson;
    private final Integer abnormalCount;
    private final Integer tokenIn;
    private final Integer tokenOut;
    private final Integer durationMs;
    private final String errorMsg;
    private final LocalDateTime createdAt;

    public static InterpretationVO from(AiInterpretation entity) {
        return InterpretationVO.builder()
                .id(entity.getId())
                .reportId(entity.getReportId())
                .status(entity.getStatus())
                .riskLevel(entity.getRiskLevel())
                .summary(entity.getSummary())
                .contentJson(entity.getContentJson())
                .abnormalCount(entity.getAbnormalCount())
                .tokenIn(entity.getTokenIn())
                .tokenOut(entity.getTokenOut())
                .durationMs(entity.getDurationMs())
                .errorMsg(entity.getErrorMsg())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
