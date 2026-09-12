package com.health.assistant.vo;

import com.health.assistant.entity.AiCallLog;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class AdminAiLogVO {

    private final Long id;
    private final Long userId;
    private final String scene;
    private final String model;
    private final String requestId;
    private final Integer promptTokens;
    private final Integer completionTokens;
    private final Integer totalTokens;
    private final BigDecimal costEstimate;
    private final Integer durationMs;
    private final Integer isStream;
    private final String status;
    private final String errorMsg;
    private final LocalDateTime createdAt;

    public static AdminAiLogVO from(AiCallLog entity) {
        return AdminAiLogVO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .scene(entity.getScene())
                .model(entity.getModel())
                .requestId(entity.getRequestId())
                .promptTokens(entity.getPromptTokens())
                .completionTokens(entity.getCompletionTokens())
                .totalTokens(entity.getTotalTokens())
                .costEstimate(entity.getCostEstimate())
                .durationMs(entity.getDurationMs())
                .isStream(entity.getIsStream())
                .status(entity.getStatus())
                .errorMsg(entity.getErrorMsg())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
