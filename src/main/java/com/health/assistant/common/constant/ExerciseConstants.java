package com.health.assistant.common.constant;

import java.util.Set;

public final class ExerciseConstants {

    public static final Set<String> WORKOUT_TYPES = Set.of(
            "RUNNING", "WALKING", "CYCLING", "SWIMMING", "STRENGTH", "YOGA", "BALL_GAME", "HIKING", "OTHER");

    public static final Set<String> INTENSITIES = Set.of("LOW", "MEDIUM", "HIGH");

    public static final Set<String> FEELINGS = Set.of("EASY", "MODERATE", "HARD");

    public static final Set<String> GLUCOSE_TYPES = Set.of("FASTING", "POSTPRANDIAL", "RANDOM");

    public static final Set<String> MOODS = Set.of("GOOD", "FAIR", "BAD");

    private ExerciseConstants() {
    }
}
