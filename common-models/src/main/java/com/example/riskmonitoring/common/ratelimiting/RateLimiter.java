package com.example.riskmonitoring.common.ratelimiting;

import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;
import java.util.Optional;

/**
 * Token Bucket Rate Limiter implementation for API protection.
 * Prevents users from abusing the transaction API.
 * 
 * Usage:
 * RateLimiter limiter = RateLimiter.getInstance();
 * if (limiter.allowRequest(userId)) {
 *     // Process transaction
 * } else {
 *     // Reject with 429 Too Many Requests
 * }
 */
public class RateLimiter {
    
    private static final RateLimiter INSTANCE = new RateLimiter();
    private static final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    
    // Rate limiting configuration
    private final int tokensPerMinute;
    private final int tokensPerSecond;
    private final int maxBurstSize;
    
    private RateLimiter() {
        // Default: 60 requests per minute = 1 per second
        // Allow burst up to 5 requests
        this.tokensPerMinute = 60;
        this.tokensPerSecond = 1;
        this.maxBurstSize = 5;
    }
    
    public static RateLimiter getInstance() {
        return INSTANCE;
    }
    
    /**
     * Check if request is allowed for a given user
     * @param userId The user identifier
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean allowRequest(String userId) {
        TokenBucket bucket = buckets.computeIfAbsent(userId, k -> 
                new TokenBucket(tokensPerMinute, maxBurstSize));
        
        return bucket.consume();
    }
    
    /**
     * Get remaining tokens for a user
     * @param userId The user identifier
     * @return Number of available tokens
     */
    public int getRemainingTokens(String userId) {
        TokenBucket bucket = buckets.get(userId);
        return bucket != null ? (int) bucket.getAvailableTokens() : tokensPerMinute;
    }
    
    /**
     * Get rate limit status for a user
     */
    public RateLimitStatus getStatus(String userId) {
        TokenBucket bucket = buckets.computeIfAbsent(userId, k -> 
                new TokenBucket(tokensPerMinute, maxBurstSize));
        
        return new RateLimitStatus(
                userId,
                tokensPerMinute,
                (int) bucket.getAvailableTokens(),
                bucket.getResetTime()
        );
    }
    
    /**
     * Reset rate limit for a user
     */
    public void resetUser(String userId) {
        buckets.remove(userId);
    }
    
    /**
     * Clear all rate limit records
     */
    public void clearAll() {
        buckets.clear();
    }
    
    /**
     * Token Bucket implementation
     */
    private static class TokenBucket {
        private double tokens;
        private final int capacity;
        private final double refillRate; // tokens per millisecond
        private long lastRefillTime;
        
        TokenBucket(int tokensPerMinute, int maxBurstSize) {
            this.capacity = maxBurstSize;
            this.tokens = capacity;
            this.refillRate = (double) tokensPerMinute / 60000; // Convert to per-millisecond
            this.lastRefillTime = System.currentTimeMillis();
        }
        
        synchronized boolean consume() {
            refill();
            
            if (tokens >= 1) {
                tokens -= 1;
                return true;
            }
            
            return false;
        }
        
        synchronized double getAvailableTokens() {
            refill();
            return Math.min(tokens, capacity);
        }
        
        synchronized long getResetTime() {
            long timeSinceLastRefill = System.currentTimeMillis() - lastRefillTime;
            long timeToFullReset = (long) ((capacity - tokens) / refillRate);
            return Math.max(0, timeToFullReset);
        }
        
        private void refill() {
            long now = System.currentTimeMillis();
            long timePassed = now - lastRefillTime;
            
            if (timePassed > 0) {
                tokens = Math.min(capacity, tokens + (timePassed * refillRate));
                lastRefillTime = now;
            }
        }
    }
    
    /**
     * Rate limit status information
     */
    public static class RateLimitStatus {
        public final String userId;
        public final int limit;
        public final int remaining;
        public final long resetTimeMs;
        
        public RateLimitStatus(String userId, int limit, int remaining, long resetTimeMs) {
            this.userId = userId;
            this.limit = limit;
            this.remaining = remaining;
            this.resetTimeMs = resetTimeMs;
        }
        
        public boolean isLimited() {
            return remaining <= 0;
        }
        
        @Override
        public String toString() {
            return "RateLimitStatus{" +
                    "userId='" + userId + '\'' +
                    ", limit=" + limit +
                    ", remaining=" + remaining +
                    ", resetTimeMs=" + resetTimeMs +
                    '}';
        }
    }
}
