package com.health.assistant.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

@Configuration
public class JacksonConfig {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String TIME_PATTERN = "HH:mm:ss";
    public static final String ZONE_ID = "Asia/Shanghai";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(TIME_PATTERN);

        return builder -> {
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializers(new LocalDateTimeSerializer(dateTimeFormatter));
            builder.serializers(new LocalDateSerializer(dateFormatter));
            builder.serializers(new LocalTimeSerializer(timeFormatter));
            builder.deserializers(new LocalDateTimeDeserializer(dateTimeFormatter));
            builder.deserializers(new LocalDateDeserializer(dateFormatter));
            builder.deserializers(new LocalTimeDeserializer(timeFormatter));
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            builder.timeZone(TimeZone.getTimeZone(ZONE_ID));
        };
    }
}
