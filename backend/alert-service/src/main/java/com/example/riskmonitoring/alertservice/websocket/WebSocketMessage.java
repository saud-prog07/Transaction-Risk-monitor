package com.example.riskmonitoring.alertservice.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * WebSocket Message DTO
 * Represents a message sent over WebSocket connection
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    /**
     * Message type/action
     * Examples: "ALERT", "TRANSACTION", "PING", "PONG", "ERROR", "CONNECTION_CONFIRMED"
     */
    @JsonProperty("action")
    private String action;

    /**
     * Message data payload
     * Can be any serializable object
     */
    @JsonProperty("data")
    private Object data;

    /**
     * Message timestamp
     */
    @JsonProperty("timestamp")
    private long timestamp = System.currentTimeMillis();

    /**
     * Custom constructor
     */
    public WebSocketMessage(String action, Object data) {
        this.action = action;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
}
