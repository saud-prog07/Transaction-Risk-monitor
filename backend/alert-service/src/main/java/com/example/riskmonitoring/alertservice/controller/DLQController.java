package com.example.riskmonitoring.alertservice.controller;

import com.example.riskmonitoring.alertservice.dto.DeadLetterMessageDTO;
import com.example.riskmonitoring.alertservice.dto.DLQRetryRequest;
import com.example.riskmonitoring.alertservice.dto.DLQStatisticsDTO;
import com.example.riskmonitoring.alertservice.service.DLQManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * REST Controller for Dead Letter Queue (DLQ) management.
 * Provides endpoints to view failed messages and initiate retry operations.
 * 
 * Security:
 * - All endpoints require ADMIN role
 * - JWT authentication required
 * - All operations are audit logged
 * - Retry actions are validated and tracked
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/dlq")
public class DLQController {
    
    private final DLQManagementService dlqService;
    
    public DLQController(DLQManagementService dlqService) {
        this.dlqService = dlqService;
    }
    
    /**
     * Retrieves DLQ statistics.
     * Shows overview of failed messages and success rates.
     * 
     * Security:
     * - Requires ADMIN role
     * - JWT authentication required
     * 
     * @return DLQ statistics including counts by status
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DLQStatisticsDTO> getStatistics() {
        log.info("DLQ statistics requested");
        
        DLQStatisticsDTO stats = dlqService.getStatistics();
        
        log.debug("DLQ statistics retrieved - total: {}, pending: {}, dead: {}",
                stats.getTotalMessages(), stats.getPendingMessages(), stats.getDeadMessages());
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Retrieves pending DLQ messages (failed messages waiting for retry).
     * 
     * Security:
     * - Requires ADMIN role
     * - Page size limited to prevent abuse
     * 
     * @param pageable pagination parameters
     * @return page of pending messages
     */
    @GetMapping("/messages/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<DeadLetterMessageDTO>> getPendingMessages(Pageable pageable) {
        log.info("Pending DLQ messages requested - page: {}, size: {}", 
                pageable.getPageNumber(), pageable.getPageSize());
        
        Page<DeadLetterMessageDTO> messages = dlqService.getPendingMessages(pageable);
        
        log.debug("Retrieved {} pending messages", messages.getNumberOfElements());
        
        return ResponseEntity.ok(messages);
    }
    
    /**
     * Retrieves DLQ messages by status.
     * 
     * Security:
     * - Requires ADMIN role
     * 
     * @param status filter by status (PENDING, RETRYING, RESOLVED, DEAD)
     * @param pageable pagination parameters
     * @return page of messages with given status
     */
    @GetMapping("/messages/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<DeadLetterMessageDTO>> getMessagesByStatus(
            @PathVariable String status,
            Pageable pageable) {
        
        log.info("DLQ messages requested with status: {} - page: {}, size: {}",
                status, pageable.getPageNumber(), pageable.getPageSize());
        
        // Validate status
        try {
            Page<DeadLetterMessageDTO> messages = dlqService.getMessagesByStatus(status, pageable);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Invalid status parameter: {}", status, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Initiates retry of a failed DLQ message.
     * 
     * Security:
     * - Requires ADMIN role
     * - All retry attempts are logged for audit trail
     * - Request is validated before processing
     * 
     * @param request retry request with message ID and optional reason
     * @return updated message with new retry status
     */
    @PostMapping("/messages/retry")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeadLetterMessageDTO> retryMessage(@Valid @RequestBody DLQRetryRequest request) {
        
        log.info("Retry requested for DLQ message: {} - reason: {}", 
                request.getMessageId(), request.getReason());
        
        try {
            // Validate request
            if (request.getMessageId() == null || request.getMessageId() <= 0) {
                log.warn("Invalid message ID in retry request: {}", request.getMessageId());
                return ResponseEntity.badRequest().build();
            }
            
            // Initiate retry
            DeadLetterMessageDTO result = dlqService.retryMessage(
                    request.getMessageId(),
                    request.getReason()
            );
            
            log.info("Retry initiated successfully for message: {}", request.getMessageId());
            
            return ResponseEntity.ok(result);
            
        } catch (IllegalStateException e) {
            log.warn("Cannot retry message {}: {}", request.getMessageId(), e.getMessage());
            return ResponseEntity.status(409).build();  // 409 Conflict - can't retry
        } catch (Exception e) {
            log.error("Error retrying message: {}", request.getMessageId(), e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * Marks a DLQ message as permanently dead (no more retries).
     * Use with caution - action is irreversible.
     * 
     * Security:
     * - Requires ADMIN role
     * - Action logged for accountability
     * 
     * @param messageId the DLQ message ID
     * @param reason reason for marking as dead
     * @return success response
     */
    @PostMapping("/messages/{messageId}/dead")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markAsDead(
            @PathVariable Long messageId,
            @RequestParam(required = false) String reason) {
        
        log.warn("Request to mark DLQ message as DEAD: {} - reason: {}", messageId, reason);
        
        try {
            dlqService.markAsDead(messageId, reason);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error marking message as dead: {}", messageId, e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * Resolves a DLQ message (marks as successfully processed).
     * 
     * Security:
     * - Requires ADMIN role
     * - Action logged for audit
     * 
     * @param messageId the DLQ message ID
     * @param notes optional resolution notes
     * @return success response
     */
    @PostMapping("/messages/{messageId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resolveMessage(
            @PathVariable Long messageId,
            @RequestParam(required = false) String notes) {
        
        log.info("Request to resolve DLQ message: {} - notes: {}", messageId, notes);
        
        try {
            dlqService.resolveMessage(messageId, notes);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error resolving message: {}", messageId, e);
            return ResponseEntity.status(500).build();
        }
    }
}
