package com.example.riskmonitoring.producer.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Configuration for JMS and IBM MQ setup.
 * NOTE: JMS is DISABLED as IBM MQ 9.3.4.1 uses javax.jms which is incompatible
 * with Spring Boot 3.4's jakarta.jms requirement.
 * 
 * All JMS beans (connectionFactory, jmsTemplate, etc.) are commented out.
 * Services use REST instead of JMS for inter-service communication.
 */
@Slf4j
@Configuration
@EnableRetry
public class JmsConfiguration {

    /**
     * Creates and configures the ObjectMapper bean for JSON serialization.     
     * Used for converting Transaction objects to JSON strings.
     *
     * @return configured ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Register Java time modules for Instant, LocalDateTime, etc.
        objectMapper.findAndRegisterModules();

        // Don't serialize dates as timestamps; use ISO format
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);   

        // Don't fail on unknown properties during deserialization
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        log.info("ObjectMapper configured for JSON serialization with ISO date format");

        return objectMapper;
    }
}


