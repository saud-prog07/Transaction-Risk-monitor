package com.example.riskmonitoring.alertservice.controller;

import com.example.riskmonitoring.alertservice.dto.TransactionTraceResponse;
import com.example.riskmonitoring.alertservice.service.TransactionTraceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for transaction trace/lifecycle endpoints.
 * Provides access to the complete journey of a transaction through the system.
 * 
 * Requires authentication and role-based authorization.
 */
@Slf4j
@RestController
@RequestMapping("/api/transactions/trace")
public class TransactionTraceController {
    
    private final TransactionTraceService transactionTraceService;
    
    public TransactionTraceController(TransactionTraceService transactionTraceService) {
        this.transactionTraceService = transactionTraceService;
    }
    
    /**
     * Retrieves the complete lifecycle trace for a transaction.
     * 
     * Shows all stages: RECEIVED → QUEUED → PROCESSED → FLAGGED → ALERTED
     * With timestamps and service information for each stage.
     * 
     * Security:
     * - Requires JWT authentication (via SecurityContext)
     * - Requires ANALYST or ADMIN role
     * - User must have access to view this alert (enforced at service layer)
     * 
     * @param transactionId the unique transaction identifier (UUID)
     * @return TransactionTraceResponse with lifecycle information
     * @throws ResourceNotFoundException if transaction not found
     */
    @GetMapping("/{transactionId}")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<TransactionTraceResponse> getTransactionTrace(
            @PathVariable UUID transactionId) {
        
        log.info("Transaction trace requested for transactionId: {}", transactionId);
        
        // Service will fetch the alert and build the trace
        TransactionTraceResponse trace = transactionTraceService.getTransactionTrace(transactionId);
        
        log.debug("Transaction trace retrieved successfully for transactionId: {}", transactionId);
        
        return ResponseEntity.ok(trace);
    }
}
