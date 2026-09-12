package com.health.assistant.support;

import com.health.assistant.common.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginRateLimiterTest {

    private final LoginRateLimiter limiter = new LoginRateLimiter();

    @Test
    void shouldBlockAfterMaxFailures() {
        for (int i = 0; i < 10; i++) {
            limiter.recordFailure("alice", "1.2.3.4");
        }

        assertThatThrownBy(() -> limiter.check("alice", "1.2.3.4"))
                .isInstanceOf(BizException.class);
    }

    @Test
    void shouldAllowWhenBelowThreshold() {
        for (int i = 0; i < 9; i++) {
            limiter.recordFailure("alice", "1.2.3.4");
        }

        assertThatCode(() -> limiter.check("alice", "1.2.3.4"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldNotBlockDifferentIp() {
        for (int i = 0; i < 10; i++) {
            limiter.recordFailure("alice", "1.2.3.4");
        }

        assertThatCode(() -> limiter.check("alice", "5.6.7.8"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldResetAfterSuccess() {
        for (int i = 0; i < 10; i++) {
            limiter.recordFailure("alice", "1.2.3.4");
        }
        limiter.reset("alice", "1.2.3.4");

        assertThatCode(() -> limiter.check("alice", "1.2.3.4"))
                .doesNotThrowAnyException();
    }
}
