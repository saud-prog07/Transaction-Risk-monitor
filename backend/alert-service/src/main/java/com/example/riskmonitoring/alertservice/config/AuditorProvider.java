package com.example.riskmonitoring.alertservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Provides audit information for entities.
 * Returns the current user/system making changes to entities.
 */
@Slf4j
@Component("auditorProvider")
public class AuditorProvider implements AuditorAware<String> {

    /**
     * Returns the current auditor (user) making changes.
     * In this context, returns "SYSTEM" for automated processes.
     *
     * @return Optional containing the current auditor
     */
    @Override
    public Optional<String> getCurrentAuditor() {
        // In a real application, you would get this from SecurityContext or authentication
        return Optional.of("SYSTEM");
    }
}
