package com.example.riskmonitoring.alertservice.domain;

import com.example.riskmonitoring.common.models.RiskLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a flagged high-risk transaction requiring attention.
 * Stores alert information for compliance, investigation, and reporting.
 */
@Entity
@Table(name = "flagged_transactions", indexes = {
        @Index(name = "idx_transaction_id", columnList = "transaction_id", unique = true),
        @Index(name = "idx_risk_level", columnList = "risk_level"),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_risk_level_created", columnList = "risk_level,created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlaggedTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * Flag to track if this alert has been reviewed.
     */
    @Column(nullable = false)
    private boolean reviewed;

    /**
     * Additional notes for investigation purposes.
     */
    @Column(length = 2000)
    private String investigationNotes;

    /**
     * Current investigation status of the alert.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status;

    /**
     * Timestamp when the alert was investigated.
     */
    @Column
    private Instant investigatedAt;

    /**
     * User ID or system identifier who performed the investigation.
     */
    @Column(length = 255)
    private String investigatedBy;

    /**
     * Audit trail of all actions performed on this alert.
     */
    @OneToMany(mappedBy = "flaggedTransaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AlertAuditLog> auditLogs = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        reviewed = false;
        if (status == null) {
            status = AlertStatus.NEW;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
