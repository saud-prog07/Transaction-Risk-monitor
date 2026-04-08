package com.example.riskmonitoring.producer.client;

import com.example.riskmonitoring.common.models.Transaction;

/**
 * Interface for publishing transaction messages to a message queue.
 */
public interface MessagePublisher {

    /**
     * Publishes a transaction message to the transaction queue.
     *
     * @param transaction the transaction to publish
     * @throws com.example.riskmonitoring.producer.exception.TransactionPublishingException
     *         if the message cannot be published
     */
    void publishTransaction(Transaction transaction);
}
