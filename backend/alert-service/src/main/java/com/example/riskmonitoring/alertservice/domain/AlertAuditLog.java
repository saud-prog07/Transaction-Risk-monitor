package com.example.riskmonitoring.alertservice.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for auditing alert investigation actions.
 * Tracks who performed what action on which alert and when.
 */
@Entity
@Table(name = "alert_audit_logs", indexes = {
        @Index(name = "idx_alert_id", columnList = "alert_id"),
        @Index(name = "idx_action_timestamp", columnList = "action_timestamp"),
        @Index(name = "idx_alert_action", columnList = "alert_id,action_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    private FlaggedTransaction flaggedTransaction;

    @Column(nullable = false)
    private String actionType; // CREATED, REVIEWED, STATUS_CHANGED, NOTES_ADDED, etc.

    @Column(nullable = false)
    private String previousStatus; // Previous alert status

    @Column(nullable = false)
    private String newStatus; // New alert status

    @Column(length = 500)
    private String description; // Description of the action

    @Column(length = 2000)
    private String notes; // Notes associated with the action

    @Column(nullable = false)
    private String performedBy; // User ID or system identifier

    @Column(nullable = false, updatable = false)
    private Instant actionTimestamp;

    @Column(name = "additional_metadata", length = 2000)
    private String metadata; // JSON or key-value metadata

    @PrePersist
    protected void onCreate() {
        actionTimestamp = Instant.now();
    }
}
