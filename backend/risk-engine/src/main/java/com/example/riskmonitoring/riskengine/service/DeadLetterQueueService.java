package com.example.riskmonitoring.riskengine.service;

import com.example.riskmonitoring.common.logging.StructuredLogger;
import com.example.riskmonitoring.riskengine.domain.DeadLetterMessage;
import com.example.riskmonitoring.riskengine.domain.DeadLetterMessage.MessageStatus;
import com.example.riskmonitoring.riskengine.repository.DeadLetterMessageRepository;
import com.example.riskmonitoring.riskengine.service.MetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing Dead Letter Queue operations.
 * Handles storage, retrieval, and retry of failed messages.
 */
@Slf4j
@Service
@Transactional
public class DeadLetterQueueService {

    private final DeadLetterMessageRepository dlqRepository;
    private final MetricsService metricsService;
    private final StructuredLogger structuredLogger = StructuredLogger.getLogger(DeadLetterQueueService.class);

    public DeadLetterQueueService(DeadLetterMessageRepository dlqRepository, MetricsService metricsService) {
        this.dlqRepository = dlqRepository;
        this.metricsService = metricsService;
    }

    /**
     * Records a failed message in the DLQ.
     *
     * @param transactionId the transaction ID (may be unknown)
     * @param originalMessage the original message content
     * @param errorMessage the error that caused the failure
     * @param errorStackTrace the full stack trace
     * @return the saved DeadLetterMessage
     */
    public DeadLetterMessage recordFailedMessage(
            String transactionId,
            String originalMessage,
            String errorMessage,
            String errorStackTrace) {

        DeadLetterMessage dlqMessage = DeadLetterMessage.builder()
                .transactionId(transactionId != null ? transactionId : "UNKNOWN")
                .originalMessage(originalMessage)
                .status(MessageStatus.PENDING)
                .errorMessage(errorMessage)
                .errorStackTrace(errorStackTrace)
                .retryCount(0)
                .maxRetries(3)
                .build();

        DeadLetterMessage saved = dlqRepository.save(dlqMessage);

        // Increment failed count metric
        metricsService.incrementFailedCount();

        structuredLogger.setTransactionId(saved.getTransactionId());
        Map<String, Object> context = new HashMap<>();
        context.put("event", "DLQ_MESSAGE_RECORDED");
        context.put("dlqId", saved.getId());
        context.put("status", saved.getStatus().toString());
        structuredLogger.info("Message recorded in Dead Letter Queue", context);

        return saved;
    }

    /**
     * Gets a retryable message and marks it as RETRYING.
     *
     * @param dlqId the DLQ message ID
     * @return the message marked as RETRYING
     */
    public Optional<DeadLetterMessage> getAndMarkAsRetrying(Long dlqId) {
        Optional<DeadLetterMessage> message = dlqRepository.findById(dlqId);

        if (message.isPresent()) {
            DeadLetterMessage dlqMessage = message.get();
            if (dlqMessage.canRetry()) {
                dlqMessage.setStatus(MessageStatus.RETRYING);
                dlqMessage.setLastRetryAt(Instant.now());
                dlqRepository.save(dlqMessage);

                structuredLogger.setTransactionId(dlqMessage.getTransactionId());
                Map<String, Object> context = new HashMap<>();
                context.put("event", "RETRY_ATTEMPT");
                context.put("attempt", dlqMessage.getRetryCount() + 1);
                context.put("maxRetries", dlqMessage.getMaxRetries());
                structuredLogger.info("Attempting message retry from DLQ", context);
            }
        }

        return message;
    }

    /**
     * Marks a message as successfully resolved after retry.
     *
     * @param dlqId the DLQ message ID
     * @param notes optional resolution notes
     */
    public void markAsResolved(Long dlqId, String notes) {
        Optional<DeadLetterMessage> message = dlqRepository.findById(dlqId);

        if (message.isPresent()) {
            DeadLetterMessage dlqMessage = message.get();
            dlqMessage.setStatus(MessageStatus.RESOLVED);
            dlqMessage.setResolvedAt(Instant.now());
            dlqMessage.setResolutionNotes(notes);
            dlqRepository.save(dlqMessage);

            structuredLogger.setTransactionId(dlqMessage.getTransactionId());
            Map<String, Object> context = new HashMap<>();
            context.put("event", "MESSAGE_RESOLVED");
            context.put("retryCount", dlqMessage.getRetryCount());
            context.put("notes", notes);
            structuredLogger.info("Message resolved and removed from DLQ", context);
        }
    }

    /**
     * Marks a message as dead (exhausted all retries).
     *
     * @param dlqId the DLQ message ID
     * @param notes optional notes about why it failed
     */
    public void markAsDead(Long dlqId, String notes) {
        Optional<DeadLetterMessage> message = dlqRepository.findById(dlqId);

        if (message.isPresent()) {
            DeadLetterMessage dlqMessage = message.get();
            dlqMessage.setStatus(MessageStatus.DEAD);
            dlqMessage.setResolvedAt(Instant.now());
            dlqMessage.setResolutionNotes(notes);
            dlqRepository.save(dlqMessage);

            structuredLogger.setTransactionId(dlqMessage.getTransactionId());
            Map<String, Object> context = new HashMap<>();
            context.put("event", "MESSAGE_MARKED_DEAD");
            context.put("retryCount", dlqMessage.getRetryCount());
            context.put("notes", notes);
            structuredLogger.error("Message marked as DEAD - exhausted all retry attempts", context);
        }
    }

    /**
     * Records a retry attempt.
     *
     * @param dlqId the DLQ message ID
     * @param success whether the retry was successful
     * @param errorMessage optional error message if retry failed
     */
    public void recordRetryAttempt(Long dlqId, boolean success, String errorMessage) {
        Optional<DeadLetterMessage> message = dlqRepository.findById(dlqId);

        if (message.isPresent()) {
            DeadLetterMessage dlqMessage = message.get();

            if (success) {
                markAsResolved(dlqId, "Successfully retried after " + dlqMessage.getRetryCount() + " attempts");
            } else {
                dlqMessage.setRetryCount(dlqMessage.getRetryCount() + 1);
                dlqMessage.setErrorMessage(errorMessage);

                if (dlqMessage.shouldMarkAsDead()) {
                    markAsDead(dlqId, "Failed after " + dlqMessage.getRetryCount() + " retry attempts");
                } else {
                    dlqMessage.setStatus(MessageStatus.PENDING);
                    dlqRepository.save(dlqMessage);

                    structuredLogger.setTransactionId(dlqMessage.getTransactionId());
                    Map<String, Object> context = new HashMap<>();
                    context.put("event", "RETRY_FAILED");
                    context.put("attempt", dlqMessage.getRetryCount());
                    context.put("maxRetries", dlqMessage.getMaxRetries());
                    context.put("error", errorMessage);
                    structuredLogger.warn("Retry failed, message returned to PENDING", context);
                }
            }
        }
    }

    /**
     * Gets all retryable messages.
     *
     * @return list of messages that can be retried
     */
    @Transactional(readOnly = true)
    public List<DeadLetterMessage> getRetryableMessages() {
        return dlqRepository.findRetryableMessages();
    }

    /**
     * Gets messages by status.
     *
     * @param status the message status filter
     * @param pageable pagination info
     * @return page of messages
     */
    @Transactional(readOnly = true)
    public Page<DeadLetterMessage> getMessagesByStatus(MessageStatus status, Pageable pageable) {
        return dlqRepository.findByStatus(status, pageable);
    }

    /**
     * Gets dead messages (exhausted all retries).
     *
     * @param pageable pagination info
     * @return page of dead messages
     */
    @Transactional(readOnly = true)
    public Page<DeadLetterMessage> getDeadMessages(Pageable pageable) {
        return dlqRepository.findDeadMessages(pageable);
    }

    /**
     * Gets messages by transaction ID.
     *
     * @param transactionId the transaction ID
     * @return the DLQ message if found
     */
    @Transactional(readOnly = true)
    public Optional<DeadLetterMessage> getMessageByTransactionId(String transactionId) {
        return dlqRepository.findByTransactionId(transactionId);
    }

    /**
     * Gets DLQ statistics.
     *
     * @return map of statistics
     */
    @Transactional(readOnly = true)
    public DLQStatistics getStatistics() {
        return DLQStatistics.builder()
                .totalMessages(dlqRepository.count())
                .pendingMessages(dlqRepository.countByStatus(MessageStatus.PENDING))
                .retryingMessages(dlqRepository.countByStatus(MessageStatus.RETRYING))
                .resolvedMessages(dlqRepository.countByStatus(MessageStatus.RESOLVED))
                .deadMessages(dlqRepository.countByStatus(MessageStatus.DEAD))
                .activeMessages(dlqRepository.countActiveMessages())
                .build();
    }

    /**
     * DLQ statistics DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class DLQStatistics {
        private long totalMessages;
        private long pendingMessages;
        private long retryingMessages;
        private long resolvedMessages;
        private long deadMessages;
        private long activeMessages;
    }
}
