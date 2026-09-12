package com.health.assistant.support;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.health.assistant.common.exception.BizException;
import com.health.assistant.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class LoginRateLimiter {

    private static final int MAX_FAILURES = 10;

    private final Cache<String, Integer> failureCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .maximumSize(10_000)
            .build();

    public void check(String username, String ip) {
        Integer failures = failureCache.getIfPresent(buildKey(username, ip));
        if (failures != null && failures >= MAX_FAILURES) {
            throw new BizException(ErrorCode.LOGIN_RATE_LIMITED);
        }
    }

    public void recordFailure(String username, String ip) {
        failureCache.asMap().merge(buildKey(username, ip), 1, Integer::sum);
    }

    public void reset(String username, String ip) {
        failureCache.invalidate(buildKey(username, ip));
    }

    private String buildKey(String username, String ip) {
        return username + "|" + ip;
    }
}
