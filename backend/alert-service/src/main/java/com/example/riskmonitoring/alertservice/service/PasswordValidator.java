package com.example.riskmonitoring.alertservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Password Validator Service
 * Enforces password strength requirements
 * 
 * Requirements:
 * - Minimum 8 characters
 * - At least one uppercase letter
 * - At least one lowercase letter
 * - At least one digit
 * - At least one special character (!@#$%^&*)
 */
@Slf4j
@Service
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final String UPPERCASE_PATTERN = ".*[A-Z].*";
    private static final String LOWERCASE_PATTERN = ".*[a-z].*";
    private static final String DIGIT_PATTERN = ".*[0-9].*";
    private static final String SPECIAL_CHAR_PATTERN = ".*[!@#$%^&*()_+\\-=\\[\\]{};':\",./<>?].*";

    /**
     * Validate password strength
     * @param password the password to validate
     * @return true if password meets all requirements
     */
    public boolean isPasswordValid(String password) {
        if (password == null) {
            return false;
        }

        // Check minimum length
        if (password.length() < MIN_LENGTH) {
            log.debug("Password failed: minimum length requirement");
            return false;
        }

        // Check uppercase requirement
        if (!password.matches(UPPERCASE_PATTERN)) {
            log.debug("Password failed: uppercase requirement");
            return false;
        }

        // Check lowercase requirement
        if (!password.matches(LOWERCASE_PATTERN)) {
            log.debug("Password failed: lowercase requirement");
            return false;
        }

        // Check digit requirement
        if (!password.matches(DIGIT_PATTERN)) {
            log.debug("Password failed: digit requirement");
            return false;
        }

        // Check special character requirement
        if (!password.matches(SPECIAL_CHAR_PATTERN)) {
            log.debug("Password failed: special character requirement");
            return false;
        }

        return true;
    }

    /**
     * Get password strength requirements message
     * @return formatted string describing requirements
     */
    public String getPasswordRequirements() {
        return "Password must contain: " +
                "minimum 8 characters, " +
                "at least one uppercase letter (A-Z), " +
                "at least one lowercase letter (a-z), " +
                "at least one digit (0-9), " +
                "at least one special character (!@#$%^&*)";
    }
}
