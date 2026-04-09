package com.example.riskmonitoring.alertservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Login Attempt Service
 * Tracks login attempts and implements rate limiting
 * 
 * Rate Limiting:
 * - Max 5 failed login attempts
 * - Within 15 minute window
 * - Account lockout after exceeding limit
 */
@Slf4j
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long ATTEMPT_INCREMENT = 1000;
    private static final long MAX_ATTEMPT_TIME = 15 * 60 * 1000; // 15 minutes in milliseconds

    private final Map<String, Integer> attemptsCache = new ConcurrentHashMap<>();
    private final Map<String, Long> lastAttemptTime = new ConcurrentHashMap<>();

    /**
     * Record a failed login attempt
     * @param username the username
     * @return true if user can still attempt login, false if rate limited
     */
    public synchronized boolean recordFailedAttempt(String username) {
        int attempts = attemptsCache.getOrDefault(username, 0);
        long lastAttempt = lastAttemptTime.getOrDefault(username, 0L);
        long now = System.currentTimeMillis();

        // Reset counter if outside the time window
        if (now - lastAttempt > MAX_ATTEMPT_TIME) {
            attempts = 0;
        }

        attempts++;
        attemptsCache.put(username, attempts);
        lastAttemptTime.put(username, now);

        log.warn("Failed login attempt #{} for user: {}", attempts, username);

        return attempts < MAX_ATTEMPTS;
    }

    /**
     * Clear attempt counter after successful login
     * @param username the username
     */
    public synchronized void clearFailedAttempts(String username) {
        attemptsCache.remove(username);
        lastAttemptTime.remove(username);
        log.info("Cleared failed login attempts for user: {}", username);
    }

    /**
     * Check if user is rate limited
     * @param username the username
     * @return true if user is rate limited, false otherwise
     */
    public synchronized boolean isRateLimited(String username) {
        Integer attempts = attemptsCache.get(username);
        Long lastAttempt = lastAttemptTime.get(username);

        if (attempts == null || lastAttempt == null) {
            return false;
        }

        long now = System.currentTimeMillis();

        // Check if outside time window
        if (now - lastAttempt > MAX_ATTEMPT_TIME) {
            attemptsCache.remove(username);
            lastAttemptTime.remove(username);
            return false;
        }

        return attempts >= MAX_ATTEMPTS;
    }

    /**
     * Get remaining time until rate limit is cleared (in seconds)
     * @param username the username
     * @return remaining seconds or 0 if not rate limited
     */
    public synchronized long getRemainingLockoutTime(String username) {
        Long lastAttempt = lastAttemptTime.get(username);

        if (lastAttempt == null) {
            return 0;
        }

        long remainingMs = MAX_ATTEMPT_TIME - (System.currentTimeMillis() - lastAttempt);
        return Math.max(0, remainingMs / 1000);
    }

    /**
     * Get current failed attempt count for user
     * @param username the username
     * @return number of failed attempts
     */
    public int getFailedAttemptCount(String username) {
        return attemptsCache.getOrDefault(username, 0);
    }
}
