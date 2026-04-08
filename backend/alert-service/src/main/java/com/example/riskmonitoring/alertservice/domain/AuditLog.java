package com.example.riskmonitoring.alertservice.domain;

import lombok.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * Audit log entity for tracking all actions performed on alerts.
 * Provides immutable audit trail with full action history.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_alert_id", columnList = "alert_id"),
    @Index(name = "idx_action_timestamp", columnList = "action_timestamp"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_action_type", columnList = "action_type"),
    @Index(name = "idx_alert_action", columnList = "alert_id,action")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = "alert")
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false, foreignKey = @ForeignKey(name = "fk_audit_alert"))
    private AlertEntity alert;
    
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private AuditAction action;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private Instant timestamp;
    
    @Column(length = 2000)
    private String details;
    
    @Column(name = "previous_status", length = 50)
    private String previousStatus;
    
    @Column(name = "new_status", length = 50)
    private String newStatus;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "request_id", length = 100)
    private String requestId;
    
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
    
    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", alertId=" + (alert != null ? alert.getId() : null) +
                ", action=" + action +
                ", userId='" + userId + '\'' +
                ", timestamp=" + timestamp +
                ", previousStatus='" + previousStatus + '\'' +
                ", newStatus='" + newStatus + '\'' +
                '}';
    }
}
