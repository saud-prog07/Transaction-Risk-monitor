package com.example.riskmonitoring.alertservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when abuse/bot activity is detected
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AbuseDetectedException extends RuntimeException {
    private final String abuseType;
    private final String identifier;

    public AbuseDetectedException(String message) {
        super(message);
        this.abuseType = "UNKNOWN";
        this.identifier = "unknown";
    }

    public AbuseDetectedException(String message, String abuseType) {
        super(message);
        this.abuseType = abuseType;
        this.identifier = "unknown";
    }

    public AbuseDetectedException(String message, String abuseType, String identifier) {
        super(message);
        this.abuseType = abuseType;
        this.identifier = identifier;
    }

    public String getAbuseType() {
        return abuseType;
    }

    public String getIdentifier() {
        return identifier;
    }
}
