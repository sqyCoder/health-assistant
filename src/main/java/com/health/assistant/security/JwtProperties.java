package com.health.assistant.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;

    private long accessExpireMinutes = 120;

    private long refreshExpireDays = 7;
}
