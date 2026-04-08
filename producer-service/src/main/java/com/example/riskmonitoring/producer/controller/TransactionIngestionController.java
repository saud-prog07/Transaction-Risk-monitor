package com.example.riskmonitoring.producer.controller;

import com.example.riskmonitoring.common.models.Transaction;
import com.example.riskmonitoring.producer.domain.TransactionIngestionRequest;
import com.example.riskmonitoring.producer.service.TransactionIngestionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for transaction ingestion.
 * Handles incoming transaction requests and publishes them to the message queue.
 */
@Slf4j
@RestController
@RequestMapping("/transaction")
public class TransactionIngestionController {

    private final TransactionIngestionService transactionIngestionService;

    public TransactionIngestionController(TransactionIngestionService transactionIngestionService) {
        this.transactionIngestionService = transactionIngestionService;
    }

    /**
     * POST endpoint for transaction ingestion.
     * Accepts transaction data, generates transactionId and timestamp,
     * converts to Transaction object, and sends to MQ.
     *
     * @param request the transaction ingestion request
     * @return ResponseEntity with the processed transaction
     */
    @PostMapping
    public ResponseEntity<Transaction> submitTransaction(
            @Valid @RequestBody TransactionIngestionRequest request) {

        log.debug("Received transaction ingestion request: {}", request);

        Transaction transaction = transactionIngestionService.ingestTransaction(request);

        log.info("Transaction ingestion completed. TransactionId: {}", transaction.getTransactionId());

        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }
}
