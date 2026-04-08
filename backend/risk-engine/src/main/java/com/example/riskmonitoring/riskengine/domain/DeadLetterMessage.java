package com.example.riskmonitoring.riskengine.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity representing a message that failed to process and was sent to DLQ.
 * Stores failed message content and metadata for investigation and retry.
 */
@Entity
@Table(name = "dead_letter_messages", indexes = {
        @Index(name = "idx_dlq_status", columnList = "status"),
        @Index(name = "idx_dlq_created_at", columnList = "created_at"),
        @Index(name = "idx_dlq_status_created", columnList = "status,created_at"),
        @Index(name = "idx_dlq_transaction_id", columnList = "transaction_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeadLetterMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "original_message", columnDefinition = "TEXT")
    private String originalMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status;  // PENDING, RETRYING, RESOLVED, DEAD

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_stacktrace", columnDefinition = "TEXT")
    private String errorStackTrace;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(name = "last_retry_at")
    private Instant lastRetryAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    public enum MessageStatus {
        PENDING,      // Waiting for retry
        RETRYING,     // Currently being retried
        RESOLVED,     // Successfully processed after retry
        DEAD          // Failed all retries, moved to dead letter
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Check if this message can be retried
     */
    public boolean canRetry() {
        return status == MessageStatus.PENDING && retryCount < maxRetries;
    }

    /**
     * Check if this message should be marked as dead
     */
    public boolean shouldMarkAsDead() {
        return retryCount >= maxRetries;
    }
}
