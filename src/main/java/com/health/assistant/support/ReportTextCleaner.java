package com.health.assistant.support;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class ReportTextCleaner {

    private static final Pattern PAGE_NUMBER_PATTERN = Pattern.compile("^\\s*(第\\s*\\d+\\s*页|\\d+\\s*/\\s*\\d+|-\\s*\\d+\\s*-)\\s*$");
    private static final int REPEATED_LINE_THRESHOLD = 3;

    public String clean(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String normalized = normalizeWidth(raw);
        String[] lines = normalized.split("\\R");
        Map<String, Integer> counts = new HashMap<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                counts.merge(trimmed, 1, Integer::sum);
            }
        }
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (PAGE_NUMBER_PATTERN.matcher(trimmed).matches()) {
                continue;
            }
            if (counts.getOrDefault(trimmed, 0) >= REPEATED_LINE_THRESHOLD) {
                continue;
            }
            result.append(trimmed).append('\n');
        }
        return result.toString();
    }

    public String normalizeWidth(String input) {
        StringBuilder builder = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            if (c == '\u3000') {
                builder.append(' ');
            } else if (c >= '０' && c <= '９') {
                builder.append((char) (c - '０' + '0'));
            } else if (c >= 'Ａ' && c <= 'Ｚ') {
                builder.append((char) (c - 'Ａ' + 'A'));
            } else if (c >= 'ａ' && c <= 'ｚ') {
                builder.append((char) (c - 'ａ' + 'a'));
            } else if (c == '（') {
                builder.append('(');
            } else if (c == '）') {
                builder.append(')');
            } else if (c == '：') {
                builder.append(':');
            } else if (c == '，') {
                builder.append(',');
            } else if (c == '．') {
                builder.append('.');
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
