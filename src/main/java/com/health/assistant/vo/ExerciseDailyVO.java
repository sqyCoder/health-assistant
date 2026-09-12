package com.health.assistant.vo;

import com.health.assistant.entity.ExerciseDaily;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class ExerciseDailyVO {

    private final Long id;
    private final LocalDate recordDate;
    private final Integer steps;
    private final Integer activeMinutes;
    private final BigDecimal caloriesKcal;
    private final BigDecimal distanceKm;
    private final BigDecimal sleepHours;
    private final String sleepQuality;
    private final Integer restingHeartRate;
    private final BigDecimal weightKg;
    private final Integer systolicBp;
    private final Integer diastolicBp;
    private final BigDecimal bloodGlucose;
    private final String glucoseType;
    private final String mood;
    private final String note;

    public static ExerciseDailyVO from(ExerciseDaily entity) {
        if (entity == null) {
            return null;
        }
        return ExerciseDailyVO.builder()
                .id(entity.getId())
                .recordDate(entity.getRecordDate())
                .steps(entity.getSteps())
                .activeMinutes(entity.getActiveMinutes())
                .caloriesKcal(entity.getCaloriesKcal())
                .distanceKm(entity.getDistanceKm())
                .sleepHours(entity.getSleepHours())
                .sleepQuality(entity.getSleepQuality())
                .restingHeartRate(entity.getRestingHeartRate())
                .weightKg(entity.getWeightKg())
                .systolicBp(entity.getSystolicBp())
                .diastolicBp(entity.getDiastolicBp())
                .bloodGlucose(entity.getBloodGlucose())
                .glucoseType(entity.getGlucoseType())
                .mood(entity.getMood())
                .note(entity.getNote())
                .build();
    }
}
