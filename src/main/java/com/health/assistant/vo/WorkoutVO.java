package com.health.assistant.vo;

import com.health.assistant.entity.ExerciseWorkout;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class WorkoutVO {

    private final Long id;
    private final LocalDate workoutDate;
    private final LocalTime startTime;
    private final String workoutType;
    private final String title;
    private final Integer durationMin;
    private final String intensity;
    private final BigDecimal distanceKm;
    private final Integer avgHeartRate;
    private final Integer maxHeartRate;
    private final Integer paceSecPerKm;
    private final BigDecimal caloriesKcal;
    private final String feeling;
    private final String note;

    public static WorkoutVO from(ExerciseWorkout entity) {
        return WorkoutVO.builder()
                .id(entity.getId())
                .workoutDate(entity.getWorkoutDate())
                .startTime(entity.getStartTime())
                .workoutType(entity.getWorkoutType())
                .title(entity.getTitle())
                .durationMin(entity.getDurationMin())
                .intensity(entity.getIntensity())
                .distanceKm(entity.getDistanceKm())
                .avgHeartRate(entity.getAvgHeartRate())
                .maxHeartRate(entity.getMaxHeartRate())
                .paceSecPerKm(entity.getPaceSecPerKm())
                .caloriesKcal(entity.getCaloriesKcal())
                .feeling(entity.getFeeling())
                .note(entity.getNote())
                .build();
    }
}
