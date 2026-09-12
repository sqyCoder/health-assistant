package com.health.assistant.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.assistant.storage.LocalDiskStorageServiceImpl;
import com.health.assistant.storage.StorageProperties;
import com.health.assistant.storage.StorageService;
import com.health.assistant.storage.SupabaseRestStorageServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = StorageProperties.TYPE_SUPABASE, matchIfMissing = true)
    public StorageService supabaseStorageService(StorageProperties properties, ObjectMapper objectMapper) {
        return new SupabaseRestStorageServiceImpl(properties.getSupabase(), objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = StorageProperties.TYPE_LOCAL)
    public StorageService localStorageService(StorageProperties properties) {
        return new LocalDiskStorageServiceImpl(properties.getLocal());
    }
}
