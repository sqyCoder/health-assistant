package com.health.assistant.security;

import com.health.assistant.common.exception.AuthException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET = "unit-test-secret-key-0123456789abcdef";

    private JwtProperties properties;
    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setAccessExpireMinutes(120);
        properties.setRefreshExpireDays(7);
        provider = new JwtTokenProvider(properties);
    }

    @Test
    void shouldCreateAndParseAccessToken() {
        String token = provider.createAccessToken(1001L, "alice", "USER");

        LoginUser loginUser = provider.parseAccessToken(token);

        assertThat(loginUser.id()).isEqualTo(1001L);
        assertThat(loginUser.username()).isEqualTo("alice");
        assertThat(loginUser.role()).isEqualTo("USER");
    }

    @Test
    void shouldParseRefreshTokenSubject() {
        String token = provider.createRefreshToken(2002L);

        assertThat(provider.parseRefreshToken(token)).isEqualTo(2002L);
    }

    @Test
    void shouldRejectRefreshTokenUsedAsAccessToken() {
        String refreshToken = provider.createRefreshToken(1001L);

        assertThatThrownBy(() -> provider.parseAccessToken(refreshToken))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void shouldRejectAccessTokenUsedAsRefreshToken() {
        String accessToken = provider.createAccessToken(1001L, "alice", "USER");

        assertThatThrownBy(() -> provider.parseRefreshToken(accessToken))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = provider.createAccessToken(1001L, "alice", "USER");
        String tampered = token.substring(0, token.lastIndexOf('.') + 1)
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

        assertThatThrownBy(() -> provider.parseAccessToken(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenSignedWithAnotherSecret() {
        JwtProperties other = new JwtProperties();
        other.setSecret("another-secret-key-0123456789abcdefgh");
        other.setAccessExpireMinutes(120);
        other.setRefreshExpireDays(7);
        JwtTokenProvider otherProvider = new JwtTokenProvider(other);
        String token = otherProvider.createAccessToken(1001L, "alice", "USER");

        assertThatThrownBy(() -> provider.parseAccessToken(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectExpiredAccessToken() {
        properties.setAccessExpireMinutes(-1);
        JwtTokenProvider expiredProvider = new JwtTokenProvider(properties);
        String token = expiredProvider.createAccessToken(1001L, "alice", "USER");

        assertThatThrownBy(() -> provider.parseAccessToken(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectShortSecret() {
        JwtProperties shortSecret = new JwtProperties();
        shortSecret.setSecret("too-short");

        assertThatThrownBy(() -> new JwtTokenProvider(shortSecret))
                .isInstanceOf(IllegalStateException.class);
    }
}
