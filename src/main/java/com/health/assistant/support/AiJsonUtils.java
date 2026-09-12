package com.health.assistant.support;

public final class AiJsonUtils {

    private AiJsonUtils() {
    }

    public static String stripCodeFences(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstLineEnd = trimmed.indexOf('\n');
            if (firstLineEnd > 0) {
                trimmed = trimmed.substring(firstLineEnd + 1);
            } else {
                trimmed = trimmed.substring(3);
            }
            int fenceEnd = trimmed.lastIndexOf("```");
            if (fenceEnd >= 0) {
                trimmed = trimmed.substring(0, fenceEnd);
            }
            trimmed = trimmed.trim();
        }
        return trimmed;
    }
}
