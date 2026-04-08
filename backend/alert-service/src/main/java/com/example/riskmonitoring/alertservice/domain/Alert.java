package com.example.riskmonitoring.alertservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Alert Domain Entity
 * Represents a flagged transaction alert in the system.
 * This is a type alias for AlertEntity to maintain consistency.
 */
@Entity
@Table(name = "alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String transactionId;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String merchantId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal riskScore;

    @Column(nullable = false)
    private String decision;

    @Column(nullable = false, length = 1000)
    private String reasons;

    @Column(nullable = false)
    private Instant flaggedAt;

    // Investigation fields
    @Column(nullable = false)
    @Builder.Default
    private String status = "NEW"; // NEW, REVIEWED, FRAUD, SAFE

    @Column(length = 2000)
    private String investigationNotes;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "investigated_by")
    private String investigatedBy;

    @Column(name = "investigated_at")
    private Instant investigatedAt;
}
