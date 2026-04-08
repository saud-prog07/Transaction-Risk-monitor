package com.example.riskmonitoring.alertservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.riskmonitoring.alertservice.service.WebSocketBroadcastService;
import com.example.riskmonitoring.alertservice.service.WebSocketBroadcastService.WebSocketStats;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket Management REST Controller
 * Provides REST endpoints for monitoring and testing WebSocket functionality
 */
@Slf4j
@RestController
@RequestMapping("/api/websocket")
public class WebSocketController {

    @Autowired
    private WebSocketBroadcastService webSocketBroadcastService;

    /**
     * Get WebSocket connection statistics
     * Useful for monitoring active connections and subscribers
     *
     * @return WebSocket statistics including active sessions and subscribers
     */
    @GetMapping("/stats")
    public ResponseEntity<WebSocketStats> getWebSocketStats() {
        log.info("Fetching WebSocket statistics");
        WebSocketStats stats = webSocketBroadcastService.getWebSocketStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get active session count
     *
     * @return Number of active WebSocket sessions
     */
    @GetMapping("/active-sessions")
    public ResponseEntity<Map<String, Integer>> getActiveSessions() {
        Map<String, Integer> response = new HashMap<>();
        response.put("activeSessions", webSocketBroadcastService.getActiveSessionCount());
        return ResponseEntity.ok(response);
    }

    /**
     * Get alert subscribers count
     *
     * @return Number of alert subscribers
     */
    @GetMapping("/alert-subscribers")
    public ResponseEntity<Map<String, Integer>> getAlertSubscribers() {
        Map<String, Integer> response = new HashMap<>();
        response.put("alertSubscribers", webSocketBroadcastService.getAlertSubscriberCount());
        return ResponseEntity.ok(response);
    }

    /**
     * Get transaction subscribers count
     *
     * @return Number of transaction subscribers
     */
    @GetMapping("/transaction-subscribers")
    public ResponseEntity<Map<String, Integer>> getTransactionSubscribers() {
        Map<String, Integer> response = new HashMap<>();
        response.put("transactionSubscribers", webSocketBroadcastService.getTransactionSubscriberCount());
        return ResponseEntity.ok(response);
    }

    /**
     * Test endpoint to broadcast a test alert via WebSocket
     * Used for debugging and testing WebSocket functionality
     *
     * @param message Optional test message
     * @return Confirmation message
     */
    @PostMapping("/test/broadcast-alert")
    public ResponseEntity<Map<String, String>> testBroadcastAlert(
            @RequestParam(value = "message", required = false, defaultValue = "Test Alert") String message) {
        try {
            Map<String, Object> testAlert = new HashMap<>();
            testAlert.put("id", System.currentTimeMillis());
            testAlert.put("message", message);
            testAlert.put("severity", "INFO");
            testAlert.put("timestamp", System.currentTimeMillis());
            testAlert.put("sourceService", "test");

            log.info("Broadcasting test alert: {}", message);
            webSocketBroadcastService.broadcastAlert(testAlert);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Test alert broadcasted to " + 
                    webSocketBroadcastService.getAlertSubscriberCount() + " subscribers");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error broadcasting test alert", e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Test endpoint to broadcast a test transaction via WebSocket
     * Used for debugging and testing WebSocket functionality
     *
     * @param amount Optional transaction amount
     * @return Confirmation message
     */
    @PostMapping("/test/broadcast-transaction")
    public ResponseEntity<Map<String, String>> testBroadcastTransaction(
            @RequestParam(value = "amount", required = false, defaultValue = "1000.00") String amount) {
        try {
            Map<String, Object> testTransaction = new HashMap<>();
            testTransaction.put("id", System.currentTimeMillis());
            testTransaction.put("amount", Double.parseDouble(amount));
            testTransaction.put("currency", "USD");
            testTransaction.put("timestamp", System.currentTimeMillis());
            testTransaction.put("status", "PROCESSED");
            testTransaction.put("sourceAccount", "test-source");
            testTransaction.put("destinationAccount", "test-destination");

            log.info("Broadcasting test transaction: {}", amount);
            webSocketBroadcastService.broadcastTransaction(testTransaction);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Test transaction broadcasted to " + 
                    webSocketBroadcastService.getTransactionSubscriberCount() + " subscribers");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error broadcasting test transaction", e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
