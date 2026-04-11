package com.example.riskmonitoring.producer.controller;

import com.example.riskmonitoring.common.models.TransactionEvent;
import com.example.riskmonitoring.common.models.RiskAlert;
import com.example.riskmonitoring.common.models.Transaction;
import com.example.riskmonitoring.producer.client.MessagePublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for publishing messages to IBM MQ.
 * Provides endpoints for transaction events and risk alerts.
 * Injects MessagePublisher interface (implemented by MessagePublisherImpl).
 */
@Slf4j
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Tag(name = "Message Publisher API", description = "API for publishing transaction events and risk alerts to IBM MQ")
public class MessagePublisherController {

    private final MessagePublisher messagePublisher;

    /**
     * Publishes a transaction to IBM MQ.
     * Converts TransactionEvent to Transaction object and publishes via interface.
     *
     * @param event The transaction event to publish
     * @return ResponseEntity with success message or error details
     */
    @PostMapping("/transaction")
    @Operation(summary = "Publish Transaction", description = "Send a transaction to the transaction queue")
    public ResponseEntity<String> publishTransaction(@RequestBody TransactionEvent event) {
        try {
            if (event == null || event.getTransactionId() == null) {
                return ResponseEntity.badRequest().body("Invalid transaction event: missing required fields");
            }
            
            // Convert TransactionEvent to Transaction and publish via interface
            Transaction transaction = Transaction.builder()
                    .transactionId(java.util.UUID.fromString(event.getTransactionId()))
                    .userId(event.getCustomerId())
                    .amount(event.getAmount())
                    .timestamp(java.time.Instant.now())
                    .location(event.getDeviceLocation())
                    .build();
            
            messagePublisher.publishTransaction(transaction);
            log.info("Transaction published successfully: {}", event.getTransactionId());
            return ResponseEntity.ok("Transaction published successfully");
        } catch (IllegalArgumentException e) {
            log.error("Invalid transaction event: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid event: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to publish transaction event after retries: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to publish transaction: " + e.getMessage());
        }
    }

    /**
     * Health check endpoint.
     *
     * @return ResponseEntity indicating service health
     */
    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Verify message publisher is operational")
    public ResponseEntity<String> health() {
        log.debug("Health check requested");
        return ResponseEntity.ok("Message Publisher service is operational");
    }
}
