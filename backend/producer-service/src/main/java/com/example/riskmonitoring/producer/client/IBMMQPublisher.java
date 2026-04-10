package com.example.riskmonitoring.producer.client;

import com.example.riskmonitoring.common.models.Transaction;
import com.example.riskmonitoring.producer.exception.TransactionPublishingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * IBM MQ implementation of MessagePublisher.
 * Publishes transaction messages to IBM MQ queue with automatic retry on failures.
 */
@Slf4j
// @Component - DISABLED: IBM MQ 9.3.4.1 uses javax.jms incompatible with Spring Boot 3.4 jakarta.jms
// Use RestTransactionClient instead
@ConditionalOnProperty(name = "spring.jms-startup-enabled", havingValue = "true", matchIfMissing = false)
public class IBMMQPublisher implements MessagePublisher {

    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;
    private final String queueName;
    private final int maxRetries;
    private final long retryDelay;

    public IBMMQPublisher(
            JmsTemplate jmsTemplate,
            ObjectMapper objectMapper,
            @Value("${mq.queue.transaction:TRANSACTION_QUEUE}") String queueName,
            @Value("${mq.retry.max-attempts:3}") int maxRetries,
            @Value("${mq.retry.initial-delay:1000}") long retryDelay) {
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
        this.queueName = queueName;
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
        
        log.info("IBMMQPublisher initialized: Queue={}, MaxRetries={}, RetryDelay={}ms",
                queueName, maxRetries, retryDelay);
    }

    /**
     * Publishes a transaction to the IBM MQ queue with automatic retry on transient failures.
     * Uses exponential backoff strategy for retries.
     *
     * @param transaction the transaction to publish
     * @throws TransactionPublishingException if the message cannot be published after all retries
     */
    @Override
    @Retryable(
            retryFor = {JmsException.class, RuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0, random = true),
            label = "publishTransaction"
    )
    public void publishTransaction(Transaction transaction) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(transaction);

            // Publish with custom properties
            jmsTemplate.convertAndSend(queueName, jsonMessage, message -> {
                // Set transaction ID as message property for tracking
                message.setStringProperty("transactionId", transaction.getTransactionId().toString());
                message.setStringProperty("userId", transaction.getUserId());
                message.setLongProperty("amount", transaction.getAmount().longValue());
                return message;
            });

            log.info("Transaction published successfully. TransactionId: {}, Queue: {}, UserId: {}",
                    transaction.getTransactionId(), queueName, transaction.getUserId());

        } catch (JmsException jmsEx) {
            log.warn("JMS exception occurred while publishing transaction. TransactionId: {}, Retrying...",
                    transaction.getTransactionId(), jmsEx);
            throw jmsEx;
            
        } catch (Exception ex) {
            log.error("Failed to serialize or publish transaction. TransactionId: {}",
                    transaction.getTransactionId(), ex);
            throw new TransactionPublishingException(
                    "Failed to publish transaction to message queue: " + ex.getMessage(), ex);
        }
    }

    /**
     * Health check method to verify MQ connection status.
     * Sends a test message to validate the connection.
     *
     * @return true if connection is healthy, false otherwise
     */
    public boolean isHealthy() {
        try {
            // Attempt a simple send with short timeout
            jmsTemplate.convertAndSend(queueName, "HEALTH_CHECK_" + System.currentTimeMillis());
            log.debug("MQ health check passed");
            return true;
        } catch (Exception ex) {
            log.warn("MQ health check failed: {}", ex.getMessage());
            return false;
        }
    }
}

