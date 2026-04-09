package com.example.riskmonitoring.alertservice.annotation;

/**
 * Types of rate limiting
 */
public enum RateLimitType {
    /**
     * Login attempts rate limiting
     */
    LOGIN,

    /**
     * Account registration rate limiting
     */
    REGISTRATION,

    /**
     * AI generation request rate limiting
     */
    AI_GENERATION,

    /**
     * General API endpoint rate limiting
     */
    GLOBAL
}
