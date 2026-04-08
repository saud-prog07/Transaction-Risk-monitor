package com.example.riskmonitoring.alertservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing configuration for entity lifecycle tracking.
 * Enables automatic tracking of creation and modification timestamps.
 */
@Slf4j
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfiguration {

    public JpaAuditingConfiguration() {
        log.info("JPA Auditing configuration initialized");
    }
}
