package com.health.assistant.common.constant;

import java.util.Set;

public final class MemberConstants {

    public static final Set<String> RELATIONS = Set.of(
            "SELF", "FATHER", "MOTHER", "SPOUSE", "CHILD", "SIBLING", "GRANDPARENT", "OTHER");

    public static final Set<String> DISEASE_STATUSES = Set.of("CURED", "TREATING", "MANAGING");

    public static final Set<String> ALLERGY_TYPES = Set.of("DRUG", "FOOD", "CONTACT", "OTHER");

    public static final Set<String> ALLERGY_SEVERITIES = Set.of("MILD", "MODERATE", "SEVERE");

    public static final Set<String> MEDICATION_ROUTES = Set.of("ORAL", "TOPICAL", "INJECTION", "OTHER");

    public static final Set<String> MEDICATION_STATUSES = Set.of("TAKING", "STOPPED");

    public static final Set<String> FAMILY_RELATIONS = Set.of(
            "FATHER", "MOTHER", "SIBLING", "GRANDPARENT", "OTHER");

    public static final Set<String> SMOKING_OPTIONS = Set.of("NEVER", "QUIT", "SMOKING");

    public static final Set<String> DRINKING_OPTIONS = Set.of("NEVER", "OCCASIONAL", "FREQUENT", "QUIT");

    public static final Set<String> SLEEP_QUALITIES = Set.of("GOOD", "FAIR", "POOR");

    public static final Set<String> STRESS_LEVELS = Set.of("LOW", "MEDIUM", "HIGH");

    private MemberConstants() {
    }
}
