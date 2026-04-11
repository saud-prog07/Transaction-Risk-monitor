package com.example.riskmonitoring.common.retry;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for retry mechanism with exponential backoff
 * Can be used for message processing, external service calls, etc.
 */
public class RetryConfig {
    
    private int maxRetries;
    private long initialDelayMs;
    private long maxDelayMs;
    private double backoffMultiplier;
    private boolean randomDelayEnabled;
    private Map<Class<? extends Exception>, Boolean> retryableExceptions;
    
    public RetryConfig() {
        this.maxRetries = 3;
        this.initialDelayMs = 1000;
        this.maxDelayMs = 30000;
        this.backoffMultiplier = 2.0;
        this.randomDelayEnabled = true;
        this.retryableExceptions = new HashMap<>();
    }
    
    // Builder pattern for fluent configuration
    
    public static RetryConfig builder() {
        return new RetryConfig();
    }
    
    public RetryConfig maxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }
    
    public RetryConfig initialDelayMs(long initialDelayMs) {
        this.initialDelayMs = initialDelayMs;
        return this;
    }
    
    public RetryConfig maxDelayMs(long maxDelayMs) {
        this.maxDelayMs = maxDelayMs;
        return this;
    }
    
    public RetryConfig backoffMultiplier(double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
        return this;
    }
    
    public RetryConfig randomDelayEnabled(boolean enabled) {
        this.randomDelayEnabled = enabled;
        return this;
    }
    
    public RetryConfig retryOn(Class<? extends Exception> exceptionClass) {
        this.retryableExceptions.put(exceptionClass, true);
        return this;
    }
    
    public RetryConfig doNotRetryOn(Class<? extends Exception> exceptionClass) {
        this.retryableExceptions.put(exceptionClass, false);
        return this;
    }
    
    // Getters
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public long getInitialDelayMs() {
        return initialDelayMs;
    }
    
    public long getMaxDelayMs() {
        return maxDelayMs;
    }
    
    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }
    
    public boolean isRandomDelayEnabled() {
        return randomDelayEnabled;
    }
    
    public Map<Class<? extends Exception>, Boolean> getRetryableExceptions() {
        return new HashMap<>(retryableExceptions);
    }
    
    /**
     * Check if an exception should be retried
     */
    public boolean shouldRetry(Exception exception) {
        if (exception == null) {
            return false;
        }
        
        Class<? extends Exception> exceptionClass = exception.getClass();
        
        // Check exact match first
        if (retryableExceptions.containsKey(exceptionClass)) {
            return retryableExceptions.get(exceptionClass);
        }
        
        // Check parent classes
        for (Class<?> throwableClass : exception.getClass().getInterfaces()) {
            if (retryableExceptions.containsKey(throwableClass)) {
                return retryableExceptions.get(throwableClass);
            }
        }
        
        return false;
    }
    
    /**
     * Calculate delay for given attempt (with exponential backoff)
     */
    public long getDelayMs(int attemptNumber) {
        if (attemptNumber <= 0) {
            return 0;
        }
        
        // Calculate exponential backoff: initialDelay * (multiplier ^ (attempt - 1))
        long delay = (long) (initialDelayMs * Math.pow(backoffMultiplier, attemptNumber - 1));
        
        // Cap at maximum delay
        delay = Math.min(delay, maxDelayMs);
        
        // Add random jitter if enabled
        if (randomDelayEnabled) {
            long jitter = (long) (delay * 0.1 * Math.random()); // Add 0-10% random jitter
            delay = delay + jitter;
        }
        
        return delay;
    }
    
    /**
     * Get predefined config for message processing
     */
    public static RetryConfig messageProcessingConfig() {
        return new RetryConfig()
                .maxRetries(3)
                .initialDelayMs(2000)
                .maxDelayMs(30000)
                .backoffMultiplier(2.0)
                .randomDelayEnabled(true)
                .retryOn(jakarta.jms.JMSException.class)
                .retryOn(org.springframework.messaging.MessagingException.class);
    }
    
    /**
     * Get predefined config for HTTP calls
     */
    public static RetryConfig httpCallConfig() {
        return new RetryConfig()
                .maxRetries(3)
                .initialDelayMs(1000)
                .maxDelayMs(10000)
                .backoffMultiplier(2.0)
                .randomDelayEnabled(true)
                .retryOn(java.net.ConnectException.class)
                .retryOn(java.net.SocketTimeoutException.class);
    }
    
    @Override
    public String toString() {
        return "RetryConfig{" +
                "maxRetries=" + maxRetries +
                ", initialDelayMs=" + initialDelayMs +
                ", maxDelayMs=" + maxDelayMs +
                ", backoffMultiplier=" + backoffMultiplier +
                ", randomDelayEnabled=" + randomDelayEnabled +
                ", retryableExceptions=" + retryableExceptions.size() +
                '}';
    }
}
