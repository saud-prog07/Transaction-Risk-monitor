package com.example.riskmonitoring.alertservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.riskmonitoring.alertservice.websocket.AlertWebSocketHandler;

/**
 * WebSocket Broadcasting Service
 * Handles real-time push of alerts and transactions to connected WebSocket clients
 * Replaces polling mechanism with server-initiated push notifications
 */
@Slf4j
@Service
public class WebSocketBroadcastService {

    @Autowired
    private AlertWebSocketHandler alertWebSocketHandler;

    /**
     * Broadcast alert to all connected clients
     * Called when a new alert is created
     *
     * @param alertData The alert object to broadcast
     */
    public void broadcastAlert(Object alertData) {
        try {
            if (alertData != null) {
                log.debug("Broadcasting alert to {} subscribers", 
                        alertWebSocketHandler.getAlertSubscriberCount());
                alertWebSocketHandler.broadcastAlert(alertData);
            }
        } catch (Exception e) {
            log.error("Error broadcasting alert", e);
        }
    }

    /**
     * Broadcast transaction to all connected clients
     * Called when a new transaction is processed
     *
     * @param transactionData The transaction object to broadcast
     */
    public void broadcastTransaction(Object transactionData) {
        try {
            if (transactionData != null) {
                log.debug("Broadcasting transaction to {} subscribers", 
                        alertWebSocketHandler.getTransactionSubscriberCount());
                alertWebSocketHandler.broadcastTransaction(transactionData);
            }
        } catch (Exception e) {
            log.error("Error broadcasting transaction", e);
        }
    }

    /**
     * Get current number of active WebSocket sessions
     * Useful for monitoring and debugging
     *
     * @return Number of active sessions
     */
    public int getActiveSessionCount() {
        return alertWebSocketHandler.getActiveSessionCount();
    }

    /**
     * Get current number of alert subscribers
     *
     * @return Number of alert subscribers
     */
    public int getAlertSubscriberCount() {
        return alertWebSocketHandler.getAlertSubscriberCount();
    }

    /**
     * Get current number of transaction subscribers
     *
     * @return Number of transaction subscribers
     */
    public int getTransactionSubscriberCount() {
        return alertWebSocketHandler.getTransactionSubscriberCount();
    }

    /**
     * Get WebSocket connection status/metrics
     *
     * @return Statistics about WebSocket connections
     */
    public WebSocketStats getWebSocketStats() {
        return new WebSocketStats(
            getActiveSessionCount(),
            getAlertSubscriberCount(),
            getTransactionSubscriberCount(),
            System.currentTimeMillis()
        );
    }

    /**
     * WebSocket Statistics DTO
     */
    public static class WebSocketStats {
        public int activeSessions;
        public int alertSubscribers;
        public int transactionSubscribers;
        public long timestamp;

        public WebSocketStats(int activeSessions, int alertSubscribers, 
                            int transactionSubscribers, long timestamp) {
            this.activeSessions = activeSessions;
            this.alertSubscribers = alertSubscribers;
            this.transactionSubscribers = transactionSubscribers;
            this.timestamp = timestamp;
        }
    }
}
