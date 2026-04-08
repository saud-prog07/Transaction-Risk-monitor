package com.example.riskmonitoring.riskengine.controller;

import com.example.riskmonitoring.riskengine.domain.DeadLetterMessage;
import com.example.riskmonitoring.riskengine.domain.DeadLetterMessage.MessageStatus;
import com.example.riskmonitoring.riskengine.service.DeadLetterQueueService;
import com.example.riskmonitoring.riskengine.service.DeadLetterQueueService.DLQStatistics;
import com.example.riskmonitoring.riskengine.service.RetryableMessageProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * REST controller for managing Dead Letter Queue operations.
 * Provides endpoints for querying DLQ messages and manual interventions.
 */
@Slf4j
@RestController
@RequestMapping("/api/dlq")
public class DeadLetterQueueController {

    private final DeadLetterQueueService dlqService;
    private final RetryableMessageProcessor retryProcessor;

    public DeadLetterQueueController(
            DeadLetterQueueService dlqService,
            RetryableMessageProcessor retryProcessor) {
        this.dlqService = dlqService;
        this.retryProcessor = retryProcessor;
    }

    /**
     * Gets DLQ statistics.
     * Shows count of messages by status and total active messages.
     *
     * @return DLQ statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<DLQStatistics> getStatistics() {
        DLQStatistics stats = dlqService.getStatistics();
        log.info("DLQ Statistics retrieved - Total: {}, Pending: {}, Retrying: {}, Resolved: {}, Dead: {}",
                stats.getTotalMessages(), stats.getPendingMessages(), stats.getRetryingMessages(),
                stats.getResolvedMessages(), stats.getDeadMessages());
        return ResponseEntity.ok(stats);
    }

    /**
     * Gets all DLQ messages with optional status filter.
     *
     * @param status optional status filter (PENDING, RETRYING, RESOLVED, DEAD)
     * @param pageable pagination info
     * @return page of DLQ messages
     */
    @GetMapping("/messages")
    public ResponseEntity<Page<DeadLetterMessage>> getMessages(
            @RequestParam(required = false) String status,
            Pageable pageable) {

        Page<DeadLetterMessage> messages;

        if (status != null && !status.isEmpty()) {
            try {
                MessageStatus messageStatus = MessageStatus.valueOf(status.toUpperCase());
                messages = dlqService.getMessagesByStatus(messageStatus, pageable);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid status filter: {}", status);
                return ResponseEntity.badRequest().build();
            }
        } else {
            messages = dlqService.getMessagesByStatus(MessageStatus.PENDING, pageable);
        }

        return ResponseEntity.ok(messages);
    }

    /**
     * Gets all dead messages (exhausted retries).
     *
     * @param pageable pagination info
     * @return page of dead messages
     */
    @GetMapping("/messages/dead")
    public ResponseEntity<Page<DeadLetterMessage>> getDeadMessages(Pageable pageable) {
        Page<DeadLetterMessage> messages = dlqService.getDeadMessages(pageable);
        return ResponseEntity.ok(messages);
    }

    /**
     * Gets a specific DLQ message by ID.
     *
     * @param id the DLQ message ID
     * @return the DLQ message if found
     */
    @GetMapping("/messages/{id}")
    public ResponseEntity<DeadLetterMessage> getMessageById(@PathVariable Long id) {
        Optional<DeadLetterMessage> message = dlqService.getMessageByTransactionId(String.valueOf(id));
        return message.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Gets a DLQ message by transaction ID.
     *
     * @param transactionId the transaction ID
     * @return the DLQ message if found
     */
    @GetMapping("/messages/by-transaction/{transactionId}")
    public ResponseEntity<DeadLetterMessage> getMessageByTransactionId(
            @PathVariable String transactionId) {
        Optional<DeadLetterMessage> message = dlqService.getMessageByTransactionId(transactionId);
        return message.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Manually triggers retry for a specific DLQ message.
     *
     * @param dlqId the DLQ message ID
     * @return success status
     */
    @PostMapping("/{dlqId}/retry")
    public ResponseEntity<Map<String, Object>> retryMessage(@PathVariable Long dlqId) {
        boolean success = retryProcessor.retryMessage(dlqId);

        if (success) {
            log.info("DLQ: Manual retry triggered - DLQ_ID: {}", dlqId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Message retry triggered successfully",
                    "dlqId", dlqId
            ));
        } else {
            log.warn("DLQ: Manual retry failed - DLQ_ID: {}", dlqId);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to trigger retry",
                    "dlqId", dlqId
            ));
        }
    }

    /**
     * Manually marks a DLQ message as dead (skips further retries).
     *
     * @param dlqId the DLQ message ID
     * @param request request body with optional notes
     * @return success status
     */
    @PostMapping("/{dlqId}/skip-retries")
    public ResponseEntity<Map<String, Object>> skipRetries(
            @PathVariable Long dlqId,
            @RequestBody Map<String, String> request) {

        String notes = request.getOrDefault("notes", "");
        retryProcessor.skipRetries(dlqId, notes);

        log.info("DLQ: Message marked as DEAD (manual skip) - DLQ_ID: {}", dlqId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Message marked as dead, retries skipped",
                "dlqId", dlqId
        ));
    }

    /**
     * Checks if DLQ retry mechanism is enabled.
     *
     * @return retry status
     */
    @GetMapping("/retry-status")
    public ResponseEntity<Map<String, Object>> getRetryStatus() {
        return ResponseEntity.ok(Map.of(
                "dlqRetryEnabled", retryProcessor.isRetryEnabled(),
                "status", retryProcessor.isRetryEnabled() ? "ACTIVE" : "DISABLED"
        ));
    }

    /**
     * Health check endpoint for DLQ monitoring.
     * Returns summary of DLQ status.
     *
     * @return health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> dlqHealth() {
        DLQStatistics stats = dlqService.getStatistics();

        boolean isHealthy = stats.getDeadMessages() < 100 && stats.getActiveMessages() < 1000;
        String status = isHealthy ? "UP" : "DEGRADED";

        return ResponseEntity.ok(Map.of(
                "status", status,
                "dlqRetryEnabled", retryProcessor.isRetryEnabled(),
                "totalMessages", stats.getTotalMessages(),
                "activeMessages", stats.getActiveMessages(),
                "deadMessages", stats.getDeadMessages(),
                "health", isHealthy ? "GOOD" : "NEEDS_ATTENTION"
        ));
    }
}
