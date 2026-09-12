package com.health.assistant.vo;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthTokenVO {

    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final long expiresInSeconds;
    private final UserVO user;
}
