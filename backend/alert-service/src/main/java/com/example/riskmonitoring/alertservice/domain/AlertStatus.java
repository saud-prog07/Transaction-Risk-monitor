package com.example.riskmonitoring.alertservice.domain;

/**
 * Enum representing the investigation status of an alert.
 * NEW -> Transaction flagged, awaiting review
 * REVIEWED -> Alert reviewed but not conclusive
 * FRAUD -> Investigation confirmed fraud
 * SAFE -> Investigation confirmed transaction is safe/legitimate
 */
public enum AlertStatus {
    NEW,
    REVIEWED,
    FRAUD,
    SAFE;

    public static AlertStatus fromString(String status) {
        if (status == null || status.isBlank()) {
            return NEW;
        }
        try {
            return AlertStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid alert status: " + status);
        }
    }

    /**
     * Check if this status is a terminal state
     */
    public boolean isTerminal() {
        return this == FRAUD || this == SAFE;
    }

    /**
     * Check if transition is valid
     */
    public boolean canTransitionTo(AlertStatus target) {
        if (this == target) {
            return true;
        }
        // Terminal states can't transition further (except back to REVIEWED for review)
        if (this.isTerminal() && target != REVIEWED) {
            return false;
        }
        return true;
    }
}
