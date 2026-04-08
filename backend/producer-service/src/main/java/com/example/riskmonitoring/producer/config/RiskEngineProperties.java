package com.example.riskmonitoring.producer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "risk-engine")
public record RiskEngineProperties(String baseUrl) {
}