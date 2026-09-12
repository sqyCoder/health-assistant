package com.health.assistant.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.health.assistant.entity.MetricDict;
import com.health.assistant.entity.MetricReferenceRange;
import com.health.assistant.mapper.MetricDictMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MetricMatcher {

    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;
    private static final double FUZZY_THRESHOLD = 0.75;
    private static final double FUZZY_MARGIN = 0.10;

    private final MetricDictMapper metricDictMapper;

    private volatile List<MetricDict> cache;
    private volatile long cacheLoadedAt;

    public record MatchResult(MetricDict metric, String matchType) {
    }

    public MatchResult match(String rawName) {
        if (!StringUtils.hasText(rawName)) {
            return null;
        }
        String normalized = normalize(rawName);
        if (normalized.isEmpty()) {
            return null;
        }
        List<MetricDict> metrics = loadEnabledMetrics();

        for (MetricDict metric : metrics) {
            if (normalized.equals(normalize(metric.getName()))
                    || normalized.equals(normalize(metric.getNameEn()))
                    || normalized.equals(normalize(metric.getCode()))) {
                return new MatchResult(metric, "EXACT");
            }
        }
        for (MetricDict metric : metrics) {
            for (String alias : aliasList(metric)) {
                if (normalized.equals(normalize(alias))) {
                    return new MatchResult(metric, "ALIAS");
                }
            }
        }
        MetricDict best = null;
        double bestScore = 0;
        double secondScore = 0;
        for (MetricDict metric : metrics) {
            for (String candidate : allCandidates(metric)) {
                String normalizedCandidate = normalize(candidate);
                if (normalizedCandidate.isEmpty()) {
                    continue;
                }
                double score = similarity(normalized, normalizedCandidate);
                if (score > bestScore) {
                    secondScore = bestScore;
                    bestScore = score;
                    best = metric;
                } else if (score > secondScore) {
                    secondScore = score;
                }
            }
        }
        if (best != null && bestScore >= FUZZY_THRESHOLD && (bestScore - secondScore) >= FUZZY_MARGIN) {
            return new MatchResult(best, "FUZZY");
        }
        return null;
    }

    private List<MetricDict> loadEnabledMetrics() {
        long now = System.currentTimeMillis();
        if (cache == null || now - cacheLoadedAt > CACHE_TTL_MS) {
            synchronized (this) {
                if (cache == null || now - cacheLoadedAt > CACHE_TTL_MS) {
                    cache = metricDictMapper.selectList(Wrappers.<MetricDict>lambdaQuery()
                            .eq(MetricDict::getStatus, 1));
                    cacheLoadedAt = now;
                }
            }
        }
        return cache;
    }

    private List<String> aliasList(MetricDict metric) {
        if (!StringUtils.hasText(metric.getAlias())) {
            return List.of();
        }
        List<String> aliases = new ArrayList<>();
        for (String part : metric.getAlias().split("[,，、;；]")) {
            if (!part.isBlank()) {
                aliases.add(part.trim());
            }
        }
        return aliases;
    }

    private List<String> allCandidates(MetricDict metric) {
        List<String> candidates = new ArrayList<>();
        if (StringUtils.hasText(metric.getName())) {
            candidates.add(metric.getName());
        }
        if (StringUtils.hasText(metric.getNameEn())) {
            candidates.add(metric.getNameEn());
        }
        candidates.addAll(aliasList(metric));
        return candidates;
    }

    private double similarity(String a, String b) {
        if (a.equals(b)) {
            return 1.0;
        }
        if (a.contains(b) || b.contains(a)) {
            int minLength = Math.min(a.length(), b.length());
            if (minLength >= 2) {
                return 0.85 + 0.1 * minLength / Math.max(a.length(), b.length());
            }
        }
        double jaccard = jaccard(a, b);
        int distance = levenshtein(a, b);
        double levenshteinScore = 1.0 - (double) distance / Math.max(a.length(), b.length());
        return Math.max(jaccard, levenshteinScore);
    }

    private double jaccard(String a, String b) {
        java.util.Set<Character> setA = new java.util.HashSet<>();
        java.util.Set<Character> setB = new java.util.HashSet<>();
        for (char c : a.toCharArray()) {
            setA.add(c);
        }
        for (char c : b.toCharArray()) {
            setB.add(c);
        }
        java.util.Set<Character> intersection = new java.util.HashSet<>(setA);
        intersection.retainAll(setB);
        java.util.Set<Character> union = new java.util.HashSet<>(setA);
        union.addAll(setB);
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String base = stripParentheses(value);
        StringBuilder builder = new StringBuilder();
        for (char c : base.toCharArray()) {
            if (Character.isWhitespace(c)) {
                continue;
            }
            if (c >= 'Ａ' && c <= 'Ｚ') {
                builder.append((char) (c - 'Ａ' + 'A'));
            } else if (c >= 'ａ' && c <= 'ｚ') {
                builder.append((char) (c - 'ａ' + 'a'));
            } else if (c >= '０' && c <= '９') {
                builder.append((char) (c - '０' + '0'));
            } else if (c == '（') {
                builder.append('(');
            } else if (c == '）') {
                builder.append(')');
            } else {
                builder.append(Character.toLowerCase(c));
            }
        }
        return builder.toString();
    }

    private String stripParentheses(String value) {
        StringBuilder result = new StringBuilder();
        int depth = 0;
        for (char c : value.toCharArray()) {
            if (c == '(' || c == '（') {
                depth++;
                continue;
            }
            if (c == ')' || c == '）') {
                if (depth > 0) {
                    depth--;
                }
                continue;
            }
            if (depth == 0) {
                result.append(c);
            }
        }
        return result.length() > 0 ? result.toString() : value;
    }
}
