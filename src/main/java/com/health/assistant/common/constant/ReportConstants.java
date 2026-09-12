package com.health.assistant.common.constant;

import java.util.Set;

public final class ReportConstants {

    public static final Set<String> REPORT_TYPES = Set.of(
            "ANNUAL", "EMPLOYMENT", "SPECIAL", "FOLLOW_UP", "OTHER");

    public static final Set<String> ABNORMAL_JUDGE_STATUSES = Set.of(
            "HIGH", "LOW", "ABNORMAL", "POSITIVE");

    public static final String JUDGE_NORMAL = "NORMAL";
    public static final String JUDGE_HIGH = "HIGH";
    public static final String JUDGE_LOW = "LOW";
    public static final String JUDGE_ABNORMAL = "ABNORMAL";
    public static final String JUDGE_POSITIVE = "POSITIVE";
    public static final String JUDGE_NEGATIVE = "NEGATIVE";
    public static final String JUDGE_UNKNOWN = "UNKNOWN";

    public static final String SOURCE_PDF = "PDF";
    public static final String SOURCE_MANUAL = "MANUAL";

    public static final String PARSE_UPLOADED = "UPLOADED";
    public static final String PARSE_PARSING = "PARSING";
    public static final String PARSE_PARSED = "PARSED";
    public static final String PARSE_CONFIRMED = "CONFIRMED";
    public static final String PARSE_FAILED = "PARSE_FAILED";
    public static final String PARSE_NEED_MANUAL = "NEED_MANUAL";

    public static final String AI_STATUS_NONE = "NONE";

    private ReportConstants() {
    }
}
