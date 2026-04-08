package com.example.riskmonitoring.riskengine.domain;

import com.example.riskmonitoring.common.models.Transaction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity for tracking transaction history.
 * Used for frequency analysis and trend detection.
 */
@Entity
@Table(name = "transaction_history", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_timestamp", columnList = "timestamp"),
        @Index(name = "idx_user_timestamp", columnList = "user_id,timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistory {

    @Id
    private UUID transactionId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private Instant createdAt;

    /**
     * Creates a TransactionHistory from a Transaction object.
     *
     * @param transaction the transaction to convert
     * @return TransactionHistory entity
     */
    public static TransactionHistory fromTransaction(Transaction transaction) {
        return TransactionHistory.builder()
                .transactionId(transaction.getTransactionId())
                .userId(transaction.getUserId())
                .amount(transaction.getAmount())
                .timestamp(transaction.getTimestamp())
                .location(transaction.getLocation())
                .createdAt(Instant.now())
                .build();
    }
}
