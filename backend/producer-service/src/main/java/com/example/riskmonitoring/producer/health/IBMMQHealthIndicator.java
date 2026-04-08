package com.example.riskmonitoring.producer.health;

import com.example.riskmonitoring.producer.client.IBMMQPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for IBM MQ connection status.
 * Used by Spring Boot Actuator to monitor MQ connectivity.
 */
@Slf4j
@Component
public class IBMMQHealthIndicator implements HealthIndicator {

    private final IBMMQPublisher ibmMQPublisher;

    public IBMMQHealthIndicator(IBMMQPublisher ibmMQPublisher) {
        this.ibmMQPublisher = ibmMQPublisher;
    }

    @Override
    public Health health() {
        try {
            if (ibmMQPublisher.isHealthy()) {
                log.debug("IBM MQ health check passed");
                return Health.up()
                        .withDetail("status", "IBM MQ connection healthy")
                        .withDetail("component", "IBM MQ Publisher")
                        .build();
            } else {
                log.warn("IBM MQ health check failed");
                return Health.down()
                        .withDetail("status", "IBM MQ connection failed")
                        .withDetail("component", "IBM MQ Publisher")
                        .build();
            }
        } catch (Exception ex) {
            log.error("Error during IBM MQ health check", ex);
            return Health.down()
                    .withDetail("error", ex.getMessage())
                    .withException(ex)
                    .build();
        }
    }
}
