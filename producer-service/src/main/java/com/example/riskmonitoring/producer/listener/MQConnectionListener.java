package com.example.riskmonitoring.producer.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.jms.event.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.jms.support.JmsUtils;
import org.springframework.stereotype.Component;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import java.time.Instant;

/**
 * Listener for IBM MQ connection events.
 * Monitors connection state and handles reconnection scenarios.
 */
@Slf4j
@Component
public class MQConnectionListener implements ExceptionListener {

    private volatile boolean isConnected = true;
    private Instant lastConnectionFailure;

    /**
     * Called when an exception occurs on the JMS connection.
     * Logs the failure and updates connection state.
     *
     * @param exception the exception that occurred
     */
    @Override
    public void onException(javax.jms.JMSException exception) {
        log.error("MQ Connection exception occurred", exception);
        
        isConnected = false;
        lastConnectionFailure = Instant.now();
        
        // Log metrics for monitoring
        log.warn("MQ Connection lost at: {}, Error: {}", lastConnectionFailure, exception.getMessage());
    }

    /**
     * Marks the connection as recovered.
     * Called when connection is successfully restored.
     */
    public void markConnectionRecovered() {
        boolean wasDisconnected = !isConnected;
        isConnected = true;
        
        if (wasDisconnected) {
            log.info("MQ Connection recovered successfully");
        }
    }

    /**
     * Checks if the connection is currently active.
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Gets the time of the last connection failure.
     *
     * @return Instant of last failure, or null if none
     */
    public Instant getLastConnectionFailure() {
        return lastConnectionFailure;
    }

    /**
     * Gets time elapsed since last connection failure.
     *
     * @return duration in milliseconds, or -1 if no failure recorded
     */
    public long getTimeSinceLastFailure() {
        if (lastConnectionFailure == null) {
            return -1;
        }
        return Instant.now().toEpochMilli() - lastConnectionFailure.toEpochMilli();
    }

    /**
     * Resets connection state metrics.
     * Useful for testing and monitoring.
     */
    public void reset() {
        isConnected = true;
        lastConnectionFailure = null;
        log.info("Connection metrics reset");
    }
}
