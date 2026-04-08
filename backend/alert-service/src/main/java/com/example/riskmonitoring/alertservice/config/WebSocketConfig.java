package com.example.riskmonitoring.alertservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.example.riskmonitoring.alertservice.websocket.AlertWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * WebSocket Configuration
 * Enables WebSocket support and registers the alert WebSocket handler
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private AlertWebSocketHandler alertWebSocketHandler;

    /**
     * Register WebSocket handlers
     * Maps the alert endpoint to the handler and enables CORS
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(alertWebSocketHandler, "/ws/alerts")
                .setAllowedOrigins("*")
                .withSockJS();  // Fallback for browsers that don't support WebSocket

        registry.addHandler(alertWebSocketHandler, "/ws/transactions")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
