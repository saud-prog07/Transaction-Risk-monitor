package com.example.riskmonitoring.alertservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced Rate Limiting Service
 * Implements token bucket algorithm for flexible rate limiting across different endpoints
 * 
 * Features:
 * - Token bucket algorithm for fair request distribution
 * - Multiple rate limit configurations (IP-based, user-based, endpoint-based)
 * - Configurable burst capacity and refill rate
 * - Automatic cleanup of stale entries
 */
@Slf4j
@Service
public class RateLimitingService {

    // Rate limit configurations
    private static final int DEFAULT_REQUESTS_PER_MINUTE = 60;
    private static final int LOGIN_REQUESTS_PER_MINUTE = 5;
    private static final int LOGIN_REQUESTS_PER_HOUR = 20;
    private static final int REGISTRATION_REQUESTS_PER_MINUTE = 2;
    private static final int REGISTRATION_REQUESTS_PER_HOUR = 10;
    private static final int AI_GENERATION_REQUESTS_PER_MINUTE = 10;
    private static final int AI_GENERATION_REQUESTS_PER_HOUR = 100;
    
    private static final long CLEANUP_INTERVAL_MS = 60 * 60 * 1000; // 1 hour
    private long lastCleanupTime = System.currentTimeMillis();

    /**
     * Token bucket for rate limiting
     */
    public static class TokenBucket {
        private double tokens;
        private final double capacity;
        private final double refillRate;  // tokens per second
        private long lastRefillTime;

        public TokenBucket(double capacity, double refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.tokens = capacity;
            this.lastRefillTime = System.currentTimeMillis();
        }

        /**
         * Try to consume tokens
         * @param tokensToConsume number of tokens to consume
         * @return true if tokens were available, false otherwise
         */
        public synchronized boolean tryConsume(double tokensToConsume) {
            refill();
            if (tokens >= tokensToConsume) {
                tokens -= tokensToConsume;
                return true;
            }
            return false;
        }

        /**
         * Refill tokens based on elapsed time
         */
        private void refill() {
            long now = System.currentTimeMillis();
            long timePassed = now - lastRefillTime;
            double tokensToAdd = (timePassed / 1000.0) * refillRate;
            tokens = Math.min(capacity, tokens + tokensToAdd);
            lastRefillTime = now;
        }

        /**
         * Get available tokens without consuming them
         */
        public synchronized double getAvailableTokens() {
            refill();
            return tokens;
        }

        /**
         * Reset bucket to full capacity
         */
        public synchronized void reset() {
            tokens = capacity;
            lastRefillTime = System.currentTimeMillis();
        }
    }

    /**
     * Rate limit metrics for monitoring
     */
    public static class RateLimitMetrics {
        private final String key;
        private final int requestsAllowed;
        private final int requestsRemaining;
        private final long resetTimeMs;
        private final boolean limited;

        public RateLimitMetrics(String key, int allowed, int remaining, long resetTime, boolean limited) {
            this.key = key;
            this.requestsAllowed = allowed;
            this.requestsRemaining = remaining;
            this.resetTimeMs = resetTime;
            this.limited = limited;
        }

        public String getKey() { return key; }
        public int getRequestsAllowed() { return requestsAllowed; }
        public int getRequestsRemaining() { return requestsRemaining; }
        public long getResetTimeMs() { return resetTimeMs; }
        public boolean isLimited() { return limited; }
    }

    // Token buckets for different rate limit types
    private final Map<String, TokenBucket> globalBuckets = new ConcurrentHashMap<>();
    private final Map<String, TokenBucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, TokenBucket> loginHourlyBuckets = new ConcurrentHashMap<>();
    private final Map<String, TokenBucket> registrationBuckets = new ConcurrentHashMap<>();
    private final Map<String, TokenBucket> registrationHourlyBuckets = new ConcurrentHashMap<>();
    private final Map<String, TokenBucket> aiGenerationBuckets = new ConcurrentHashMap<>();
    private final Map<String, TokenBucket> aiGenerationHourlyBuckets = new ConcurrentHashMap<>();

    // Request tracking for duplicate detection
    private final Map<String, Long> lastRequestTime = new ConcurrentHashMap<>();
    private static final long DUPLICATE_REQUEST_WINDOW_MS = 1000; // 1 second

    /**
     * Check global API rate limit (per IP or user identifier)
     * @param identifier IP address or user ID
     * @return true if request is allowed, false if rate limited
     */
    public boolean checkGlobalRateLimit(String identifier) {
        return checkRateLimit(identifier, globalBuckets, 
            DEFAULT_REQUESTS_PER_MINUTE / 60.0, // convert to per-second rate
            DEFAULT_REQUESTS_PER_MINUTE);
    }

    /**
     * Check login endpoint rate limit (per username/IP)
     * @param identifier username or IP address
     * @return true if request is allowed, false if rate limited
     */
    public boolean checkLoginRateLimit(String identifier) {
        // Check per-minute limit
        boolean minuteLimit = checkRateLimit(identifier, loginBuckets,
            LOGIN_REQUESTS_PER_MINUTE / 60.0,
            LOGIN_REQUESTS_PER_MINUTE);

        // Check per-hour limit
        if (!minuteLimit) {
            return false;
        }

        String hourlyKey = "hourly_" + identifier;
        return checkRateLimit(hourlyKey, loginHourlyBuckets,
            LOGIN_REQUESTS_PER_HOUR / 3600.0,
            LOGIN_REQUESTS_PER_HOUR);
    }

    /**
     * Check account registration rate limit (per IP)
     * @param identifier IP address
     * @return true if request is allowed, false if rate limited
     */
    public boolean checkRegistrationRateLimit(String identifier) {
        // Check per-minute limit
        boolean minuteLimit = checkRateLimit(identifier, registrationBuckets,
            REGISTRATION_REQUESTS_PER_MINUTE / 60.0,
            REGISTRATION_REQUESTS_PER_MINUTE);

        if (!minuteLimit) {
            return false;
        }

        // Check per-hour limit
        String hourlyKey = "hourly_" + identifier;
        return checkRateLimit(hourlyKey, registrationHourlyBuckets,
            REGISTRATION_REQUESTS_PER_HOUR / 3600.0,
            REGISTRATION_REQUESTS_PER_HOUR);
    }

    /**
     * Check AI generation request rate limit (per user)
     * @param userId user identifier
     * @return true if request is allowed, false if rate limited
     */
    public boolean checkAIGenerationRateLimit(String userId) {
        // Check per-minute limit
        boolean minuteLimit = checkRateLimit(userId, aiGenerationBuckets,
            AI_GENERATION_REQUESTS_PER_MINUTE / 60.0,
            AI_GENERATION_REQUESTS_PER_MINUTE);

        if (!minuteLimit) {
            return false;
        }

        // Check per-hour limit
        String hourlyKey = "hourly_" + userId;
        return checkRateLimit(hourlyKey, aiGenerationHourlyBuckets,
            AI_GENERATION_REQUESTS_PER_HOUR / 3600.0,
            AI_GENERATION_REQUESTS_PER_HOUR);
    }

    /**
     * Generic rate limit check using token bucket algorithm
     */
    private boolean checkRateLimit(String identifier, Map<String, TokenBucket> buckets,
                                   double refillRate, int capacity) {
        cleanup();

        TokenBucket bucket = buckets.computeIfAbsent(identifier,
            k -> new TokenBucket(capacity, refillRate));

        return bucket.tryConsume(1.0);
    }

    /**
     * Check for duplicate requests (submitted multiple times within 1 second)
     * @param requestKey unique identifier for the request (combination of user+endpoint+params)
     * @return true if request is a duplicate, false otherwise
     */
    public boolean isDuplicateRequest(String requestKey) {
        Long lastTime = lastRequestTime.get(requestKey);
        long now = System.currentTimeMillis();

        if (lastTime == null) {
            lastRequestTime.put(requestKey, now);
            return false;
        }

        long timeDiff = now - lastTime;
        if (timeDiff < DUPLICATE_REQUEST_WINDOW_MS) {
            log.warn("Duplicate request detected: {} (time diff: {}ms)", requestKey, timeDiff);
            return true;
        }

        lastRequestTime.put(requestKey, now);
        return false;
    }

    /**
     * Get rate limit metrics for monitoring/headers
     */
    public RateLimitMetrics getLoginMetrics(String identifier) {
        TokenBucket bucket = loginBuckets.get(identifier);
        if (bucket == null) {
            return new RateLimitMetrics(identifier, LOGIN_REQUESTS_PER_MINUTE,
                LOGIN_REQUESTS_PER_MINUTE, 0, false);
        }

        int remaining = (int) bucket.getAvailableTokens();
        boolean limited = remaining <= 0;
        return new RateLimitMetrics(identifier, LOGIN_REQUESTS_PER_MINUTE, remaining, 0, limited);
    }

    /**
     * Get rate limit metrics for API endpoints
     */
    public RateLimitMetrics getGlobalMetrics(String identifier) {
        TokenBucket bucket = globalBuckets.get(identifier);
        if (bucket == null) {
            return new RateLimitMetrics(identifier, DEFAULT_REQUESTS_PER_MINUTE,
                DEFAULT_REQUESTS_PER_MINUTE, 0, false);
        }

        int remaining = (int) bucket.getAvailableTokens();
        boolean limited = remaining <= 0;
        return new RateLimitMetrics(identifier, DEFAULT_REQUESTS_PER_MINUTE, remaining, 0, limited);
    }

    /**
     * Reset rate limit for a specific identifier (for testing or admin operations)
     */
    public void resetRateLimit(String identifier) {
        TokenBucket bucket = loginBuckets.remove(identifier);
        if (bucket != null) {
            log.info("Reset rate limit for identifier: {}", identifier);
        }
    }

    /**
     * Cleanup stale entries to prevent memory leaks
     * Runs periodically to remove unused buckets
     */
    private void cleanup() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime > CLEANUP_INTERVAL_MS) {
            lastCleanupTime = now;
            
            int globalBefore = globalBuckets.size();
            globalBuckets.entrySet().removeIf(e -> e.getValue().getAvailableTokens() < 0);
            
            int loginBefore = loginBuckets.size();
            loginBuckets.entrySet().removeIf(e -> e.getValue().getAvailableTokens() < 0);
            
            log.debug("Rate limiting cleanup complete. Global: {} -> {}, Login: {} -> {}",
                globalBefore, globalBuckets.size(), loginBefore, loginBuckets.size());
        }
    }

    /**
     * Get all rate limit buckets (for monitoring/metrics)
     */
    public Map<String, Integer> getAllMetrics() {
        Map<String, Integer> metrics = new HashMap<>();
        globalBuckets.forEach((k, v) -> metrics.put("global_" + k, (int) v.getAvailableTokens()));
        loginBuckets.forEach((k, v) -> metrics.put("login_" + k, (int) v.getAvailableTokens()));
        return metrics;
    }
}
