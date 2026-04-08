package com.example.riskmonitoring.alertservice.domain;

/**
 * Enumeration of audit actions that can be performed on alerts.
 */
public enum AuditAction {
    CREATED("Alert created"),
    REVIEWED("Alert marked as reviewed"),
    UPDATED("Alert updated"),
    STATUS_CHANGED("Status changed"),
    NOTES_ADDED("Investigation notes added"),
    APPROVED("Alert approved"),
    REJECTED("Alert rejected"),
    ESCALATED("Alert escalated"),
    RESOLVED("Alert resolved"),
    REOPENED("Alert reopened"),
    DELETED("Alert deleted"),
    EXPORTED("Alert exported"),
    SHARED("Alert shared with team");
    
    private final String description;
    
    AuditAction(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
