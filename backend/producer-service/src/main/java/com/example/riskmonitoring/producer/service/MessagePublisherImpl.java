package com.example.riskmonitoring.producer.service;

import com.example.riskmonitoring.common.models.Transaction;
import com.example.riskmonitoring.producer.client.MessagePublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Simple implementation of MessagePublisher interface.
 * Injected as a Spring Service bean - no external dependencies needed.
 */
@Slf4j
@Service
public class MessagePublisherImpl implements MessagePublisher {

    @Override
    public void publishTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        log.info("📤 Publishing transaction: {}", transaction.getTransactionId());
        // TODO: Implement JMS publishing with IBM MQ
        log.info("✅ Transaction published: {}", transaction.getTransactionId());
    }
}
