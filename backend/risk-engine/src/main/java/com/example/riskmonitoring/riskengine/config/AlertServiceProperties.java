package com.example.riskmonitoring.riskengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "alert-service")
public record AlertServiceProperties(String baseUrl) {
}