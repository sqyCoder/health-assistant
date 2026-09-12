package com.health.assistant.vo;

import com.health.assistant.entity.AiSuggestion;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SuggestionVO {

    private final Long id;
    private final Long memberId;
    private final String suggestionType;
    private final String sourceType;
    private final Long sourceId;
    private final String goal;
    private final String status;
    private final String assessmentMd;
    private final String weeklyPlanJson;
    private final String rehabJson;
    private final String precautionsMd;
    private final String progressMetricsJson;
    private final Integer tokenIn;
    private final Integer tokenOut;
    private final Integer durationMs;
    private final String errorMsg;
    private final LocalDateTime createdAt;

    public static SuggestionVO from(AiSuggestion entity) {
        return SuggestionVO.builder()
                .id(entity.getId())
                .memberId(entity.getMemberId())
                .suggestionType(entity.getSuggestionType())
                .sourceType(entity.getSourceType())
                .sourceId(entity.getSourceId())
                .goal(entity.getGoal())
                .status(entity.getStatus())
                .assessmentMd(entity.getAssessmentMd())
                .weeklyPlanJson(entity.getWeeklyPlanJson())
                .rehabJson(entity.getRehabJson())
                .precautionsMd(entity.getPrecautionsMd())
                .progressMetricsJson(entity.getProgressMetricsJson())
                .tokenIn(entity.getTokenIn())
                .tokenOut(entity.getTokenOut())
                .durationMs(entity.getDurationMs())
                .errorMsg(entity.getErrorMsg())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
