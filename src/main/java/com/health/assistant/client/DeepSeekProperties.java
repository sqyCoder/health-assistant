package com.health.assistant.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekProperties {

    private String baseUrl = "https://api.deepseek.com";

    private String apiKey;

    private String model = "deepseek-chat";

    private double priceInputPerM = 2.0;

    private double priceOutputPerM = 8.0;

    private int connectTimeoutSeconds = 10;

    private int readTimeoutSeconds = 90;
}
