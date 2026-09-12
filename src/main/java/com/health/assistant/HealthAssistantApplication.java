package com.health.assistant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

import java.util.TimeZone;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@MapperScan("com.health.assistant.mapper")
public class HealthAssistantApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        SpringApplication.run(HealthAssistantApplication.class, args);
    }
}
