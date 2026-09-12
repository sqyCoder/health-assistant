package com.health.assistant.security;

import com.health.assistant.common.exception.AuthException;
import com.health.assistant.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    public static final String CLAIM_TYPE = "typ";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_USERNAME = "username";

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        byte[] keyBytes = properties.getSecret() == null
                ? new byte[0]
                : properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(Long userId, String username, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getAccessExpireMinutes(), ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_TYPE, TOKEN_TYPE_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getRefreshExpireDays(), ChronoUnit.DAYS)))
                .signWith(key)
                .compact();
    }

    public LoginUser parseAccessToken(String token) {
        Claims claims = parseClaims(token);
        if (!TOKEN_TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new AuthException(ErrorCode.UNAUTHORIZED, "非法的访问令牌");
        }
        return new LoginUser(
                Long.valueOf(claims.getSubject()),
                claims.get(CLAIM_USERNAME, String.class),
                claims.get(CLAIM_ROLE, String.class));
    }

    public Long parseRefreshToken(String token) {
        Claims claims = parseClaims(token);
        if (!TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new AuthException(ErrorCode.REFRESH_TOKEN_INVALID, "非法的刷新令牌");
        }
        return Long.valueOf(claims.getSubject());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
