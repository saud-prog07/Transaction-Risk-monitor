package com.example.riskmonitoring.alertservice.annotation;

import java.lang.annotation.*;

/**
 * Rate limiting annotation for endpoints
 * Can be applied to controller methods to enable rate limiting
 * 
 * Usage:
 * @RateLimitProtected(type = RateLimitType.LOGIN)
 * public ResponseEntity<?> login(@RequestBody LoginRequest request) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimitProtected {
    
    /**
     * Type of rate limiting to apply
     */
    RateLimitType type();

    /**
     * Number of requests allowed (0 = use default)
     */
    int requestsPerMinute() default 0;

    /**
     * Custom identifier extractor class (optional)
     */
    Class<? extends IdentifierExtractor> identifierExtractor() default DefaultIdentifierExtractor.class;

    /**
     * Whether to include User-Agent validation
     */
    boolean validateUserAgent() default true;

    /**
     * Whether to block on suspected bot activity
     */
    boolean blockBots() default true;
}

/**
 * Default identifier extractor (uses IP address)
 */
class DefaultIdentifierExtractor implements IdentifierExtractor {
    @Override
    public String extractIdentifier() {
        // This will be implemented in the aspect
        return null;
    }
}
