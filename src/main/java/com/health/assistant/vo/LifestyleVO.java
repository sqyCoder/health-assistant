package com.health.assistant.vo;

import com.health.assistant.entity.MemberLifestyle;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class LifestyleVO {

    private final String smoking;
    private final String smokingAmount;
    private final String drinking;
    private final String drinkingAmount;
    private final BigDecimal sleepHours;
    private final String sleepQuality;
    private final String exerciseFrequency;
    private final BigDecimal sedentaryHours;
    private final String dietHabit;
    private final String stressLevel;

    public static LifestyleVO from(MemberLifestyle entity) {
        if (entity == null) {
            return null;
        }
        return LifestyleVO.builder()
                .smoking(entity.getSmoking())
                .smokingAmount(entity.getSmokingAmount())
                .drinking(entity.getDrinking())
                .drinkingAmount(entity.getDrinkingAmount())
                .sleepHours(entity.getSleepHours())
                .sleepQuality(entity.getSleepQuality())
                .exerciseFrequency(entity.getExerciseFrequency())
                .sedentaryHours(entity.getSedentaryHours())
                .dietHabit(entity.getDietHabit())
                .stressLevel(entity.getStressLevel())
                .build();
    }
}
