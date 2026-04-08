package com.example.riskmonitoring.producer.controller;

import com.example.riskmonitoring.common.logging.StructuredLogger;
import com.example.riskmonitoring.common.models.TransactionRequest;
import com.example.riskmonitoring.common.models.TransactionSubmissionResponse;
import com.example.riskmonitoring.producer.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final StructuredLogger structuredLogger;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
        this.structuredLogger = StructuredLogger.getLogger(TransactionController.class);
    }

    @PostMapping
    public ResponseEntity<TransactionSubmissionResponse> submit(@RequestBody TransactionRequest request) {
        long startTime = System.currentTimeMillis();
        String transactionId = request.transactionId() != null ? request.transactionId() : "UNKNOWN";
        
        structuredLogger.logTransactionReceived(
                transactionId,
                request.accountId(),
                request.amount().doubleValue()
        );
        
        try {
            TransactionSubmissionResponse response = transactionService.submit(request);
            long duration = System.currentTimeMillis() - startTime;
            structuredLogger.setTransactionId(response.transactionId());
            structuredLogger.logProcessingComplete(response.transactionId(), duration);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception ex) {
            structuredLogger.error("Transaction processing failed", 
                    new java.util.HashMap<String, Object>() {{
                        put("transactionId", transactionId);
                        put("error", "SUBMISSION_ERROR");
                    }}, ex);
            throw ex;
        } finally {
            structuredLogger.clearContext();
        }
    }
}