package com.example.riskmonitoring.alertservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when rate limit is exceeded
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class RateLimitExceededException extends RuntimeException {
    private final long retryAfterSeconds;
    private final String identifier;

    public RateLimitExceededException(String message) {
        super(message);
        this.retryAfterSeconds = 60;
        this.identifier = "unknown";
    }

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
        this.identifier = "unknown";
    }

    public RateLimitExceededException(String message, String identifier, long retryAfterSeconds) {
        super(message);
        this.identifier = identifier;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public String getIdentifier() {
        return identifier;
    }
}
