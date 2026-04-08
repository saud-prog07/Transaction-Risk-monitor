package com.example.riskmonitoring.alertservice.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Alert WebSocket Handler
 * Manages WebSocket connections and handles incoming/outgoing messages
 * Maintains active connections for real-time message broadcasting
 */
@Slf4j
@Component
public class AlertWebSocketHandler extends TextWebSocketHandler {

    /**
     * Map to store active WebSocket sessions by session ID
     * Key: sessionId, Value: WebSocketSession
     */
    private final ConcurrentHashMap<String, WebSocketSession> activeSessions = 
            new ConcurrentHashMap<>();

    /**
     * List of sessions subscribed to alerts
     */
    private final CopyOnWriteArrayList<WebSocketSession> alertSubscribers = 
            new CopyOnWriteArrayList<>();

    /**
     * List of sessions subscribed to transactions
     */
    private final CopyOnWriteArrayList<WebSocketSession> transactionSubscribers = 
            new CopyOnWriteArrayList<>();

    /**
     * JSON object mapper for serialization
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Handle new WebSocket connection
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        activeSessions.put(sessionId, session);
        
        log.info("WebSocket connection established - Session ID: {}, Total Sessions: {}", 
                sessionId, activeSessions.size());

        // Send connection confirmation
        sendConnectionConfirmation(session);
    }

    /**
     * Handle incoming WebSocket messages
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            log.debug("Received WebSocket message from {}: {}", session.getId(), payload);

            // Parse the message to determine subscription type
            WebSocketMessage wsMessage = objectMapper.readValue(payload, WebSocketMessage.class);
            
            String action = wsMessage.getAction();
            String sessionId = session.getId();

            switch (action) {
                case "SUBSCRIBE_ALERTS":
                    if (!alertSubscribers.contains(session)) {
                        alertSubscribers.add(session);
                        log.info("Client {} subscribed to alerts. Total alert subscribers: {}", 
                                sessionId, alertSubscribers.size());
                    }
                    break;

                case "SUBSCRIBE_TRANSACTIONS":
                    if (!transactionSubscribers.contains(session)) {
                        transactionSubscribers.add(session);
                        log.info("Client {} subscribed to transactions. Total transaction subscribers: {}", 
                                sessionId, transactionSubscribers.size());
                    }
                    break;

                case "UNSUBSCRIBE_ALERTS":
                    alertSubscribers.remove(session);
                    log.info("Client {} unsubscribed from alerts. Total alert subscribers: {}", 
                            sessionId, alertSubscribers.size());
                    break;

                case "UNSUBSCRIBE_TRANSACTIONS":
                    transactionSubscribers.remove(session);
                    log.info("Client {} unsubscribed from transactions. Total transaction subscribers: {}", 
                            sessionId, transactionSubscribers.size());
                    break;

                case "PING":
                    // Send pong response to keep connection alive
                    WebSocketMessage pongMessage = new WebSocketMessage("PONG", null);
                    sendMessage(session, pongMessage);
                    break;

                default:
                    log.warn("Unknown action received: {}", action);
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
            sendErrorMessage(session, "Error processing message: " + e.getMessage());
        }
    }

    /**
     * Handle WebSocket connection closure
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        activeSessions.remove(sessionId);
        alertSubscribers.remove(session);
        transactionSubscribers.remove(session);

        log.info("WebSocket connection closed - Session ID: {}, Total Sessions: {}, Code: {}", 
                sessionId, activeSessions.size(), status.getCode());
    }

    /**
     * Handle WebSocket errors
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error for session {}", session.getId(), exception);
    }

    /**
     * Broadcast alert to all subscribers
     */
    public void broadcastAlert(Object alertData) {
        broadcastToSubscribers(alertSubscribers, "ALERT", alertData);
    }

    /**
     * Broadcast transaction to all subscribers
     */
    public void broadcastTransaction(Object transactionData) {
        broadcastToSubscribers(transactionSubscribers, "TRANSACTION", transactionData);
    }

    /**
     * Broadcast message to specific subscribers
     */
    private void broadcastToSubscribers(CopyOnWriteArrayList<WebSocketSession> subscribers, 
                                       String type, Object data) {
        WebSocketMessage message = new WebSocketMessage(type, data);
        
        for (WebSocketSession session : subscribers) {
            if (session.isOpen()) {
                try {
                    sendMessage(session, message);
                } catch (Exception e) {
                    log.error("Error broadcasting {} to session {}", type, session.getId(), e);
                    subscribers.remove(session);
                }
            } else {
                subscribers.remove(session);
            }
        }

        log.debug("Broadcasted {} to {} subscribers", type, subscribers.size());
    }

    /**
     * Send message to specific session
     */
    public void sendMessage(WebSocketSession session, WebSocketMessage message) throws IOException {
        if (session.isOpen()) {
            String payload = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(payload));
        }
    }

    /**
     * Send connection confirmation
     */
    private void sendConnectionConfirmation(WebSocketSession session) throws IOException {
        WebSocketMessage confirmation = new WebSocketMessage(
            "CONNECTION_CONFIRMED",
            java.util.Map.of(
                "sessionId", session.getId(),
                "timestamp", System.currentTimeMillis()
            )
        );
        sendMessage(session, confirmation);
    }

    /**
     * Send error message to session
     */
    private void sendErrorMessage(WebSocketSession session, String errorMsg) {
        try {
            WebSocketMessage errorMessage = new WebSocketMessage(
                "ERROR",
                java.util.Map.of("error", errorMsg)
            );
            sendMessage(session, errorMessage);
        } catch (IOException e) {
            log.error("Failed to send error message", e);
        }
    }

    /**
     * Get number of active sessions
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    /**
     * Get number of alert subscribers
     */
    public int getAlertSubscriberCount() {
        return alertSubscribers.size();
    }

    /**
     * Get number of transaction subscribers
     */
    public int getTransactionSubscriberCount() {
        return transactionSubscribers.size();
    }
}
