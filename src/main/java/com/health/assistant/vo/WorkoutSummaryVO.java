package com.health.assistant.vo;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class WorkoutSummaryVO {

    private final long totalCount;

    private final long totalMinutes;

    private final BigDecimal totalCalories;

    private final BigDecimal totalDistanceKm;

    private final List<TypeStat> byType;

    @Getter
    @Builder
    public static class TypeStat {

        private final String type;

        private final long count;

        private final long minutes;

        private final BigDecimal calories;
    }
}
