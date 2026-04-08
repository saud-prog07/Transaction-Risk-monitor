package com.example.riskmonitoring.riskengine.repository;

import com.example.riskmonitoring.riskengine.domain.DeadLetterMessage;
import com.example.riskmonitoring.riskengine.domain.DeadLetterMessage.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Dead Letter Messages.
 */
@Repository
public interface DeadLetterMessageRepository extends JpaRepository<DeadLetterMessage, Long> {

    /**
     * Find a dead letter message by transaction ID.
     */
    Optional<DeadLetterMessage> findByTransactionId(String transactionId);

    /**
     * Find all messages with a specific status.
     */
    Page<DeadLetterMessage> findByStatus(MessageStatus status, Pageable pageable);

    /**
     * Find all pending messages that can be retried.
     */
    @Query("SELECT m FROM DeadLetterMessage m WHERE m.status = 'PENDING' AND m.retryCount < m.maxRetries ORDER BY m.createdAt ASC")
    List<DeadLetterMessage> findRetryableMessages();

    /**
     * Find all dead messages (exhausted retries).
     */
    @Query("SELECT m FROM DeadLetterMessage m WHERE m.status = 'DEAD' ORDER BY m.createdAt DESC")
    Page<DeadLetterMessage> findDeadMessages(Pageable pageable);

    /**
     * Find messages created after a certain time.
     */
    Page<DeadLetterMessage> findByCreatedAtAfter(Instant createdAt, Pageable pageable);

    /**
     * Count messages by status.
     */
    long countByStatus(MessageStatus status);

    /**
     * Count all pending and retrying messages.
     */
    @Query("SELECT COUNT(m) FROM DeadLetterMessage m WHERE m.status IN ('PENDING', 'RETRYING')")
    long countActiveMessages();
}
