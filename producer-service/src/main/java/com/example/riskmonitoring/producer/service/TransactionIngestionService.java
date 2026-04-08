package com.example.riskmonitoring.producer.service;

import com.example.riskmonitoring.common.models.Transaction;
import com.example.riskmonitoring.producer.client.MessagePublisher;
import com.example.riskmonitoring.producer.domain.TransactionIngestionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Service for handling transaction ingestion.
 * Responsible for converting requests to Transaction objects and publishing them to the message queue.
 */
@Slf4j
@Service
public class TransactionIngestionService {

    private final MessagePublisher messagePublisher;

    public TransactionIngestionService(MessagePublisher messagePublisher) {
        this.messagePublisher = messagePublisher;
    }

    /**
     * Ingests a transaction from a request, generates necessary metadata,
     * and publishes it to the transaction queue.
     *
     * @param request the transaction ingestion request
     * @return the persisted Transaction object
     */
    public Transaction ingestTransaction(TransactionIngestionRequest request) {
        try {
            // Generate transaction ID and timestamp
            UUID transactionId = UUID.randomUUID();
            Instant timestamp = Instant.now();

            // Convert request to Transaction object
            Transaction transaction = Transaction.builder()
                    .transactionId(transactionId)
                    .userId(request.getUserId())
                    .amount(request.getAmount())
                    .timestamp(timestamp)
                    .location(request.getLocation())
                    .build();

            // Publish to message queue
            messagePublisher.publishTransaction(transaction);

            log.info("Transaction ingested successfully. TransactionId: {}, UserId: {}, Amount: {}",
                    transactionId, request.getUserId(), request.getAmount());

            return transaction;

        } catch (Exception ex) {
            log.error("Error during transaction ingestion: {}", ex.getMessage(), ex);
            throw ex;
        }
    }
}
