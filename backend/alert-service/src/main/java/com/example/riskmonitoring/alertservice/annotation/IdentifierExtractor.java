package com.example.riskmonitoring.alertservice.annotation;

/**
 * Interface for custom identifier extraction
 */
public interface IdentifierExtractor {
    /**
     * Extract identifier from request context
     * @return identifier for rate limiting (e.g., IP address, user ID, etc.)
     */
    String extractIdentifier();
}
