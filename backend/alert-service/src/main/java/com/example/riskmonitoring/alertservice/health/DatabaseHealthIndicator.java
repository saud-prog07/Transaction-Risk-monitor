package com.example.riskmonitoring.alertservice.health;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Health indicator for database and connection pool.
 * Monitors database connectivity and HikariCP pool status.
 */
@Slf4j
@Component("databaseHealth")
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try {
            // Test database connectivity
            try (Connection connection = dataSource.getConnection()) {
                if (connection.isValid(5)) {
                    log.debug("Database health check passed");
                    return buildHealthStatus();
                } else {
                    log.warn("Database connection validation failed");
                    return Health.down()
                            .withDetail("error", "Connection validation failed")
                            .build();
                }
            }
        } catch (Exception ex) {
            log.error("Database health check failed", ex);
            return Health.down()
                    .withDetail("error", ex.getMessage())
                    .withException(ex)
                    .build();
        }
    }

    /**
     * Builds detailed health status with connection pool information.
     */
    private Health buildHealthStatus() {
        Health.Builder builder = new Health.Builder().up();

        // Add HikariCP pool details if available
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;

            builder.withDetail("database", "PostgreSQL")
                    .withDetail("pool_size", hikariDataSource.getHikariPoolMXBean().getPoolSize())
                    .withDetail("active_connections", hikariDataSource.getHikariPoolMXBean().getActiveConnections())
                    .withDetail("idle_connections", hikariDataSource.getHikariPoolMXBean().getIdleConnections())
                    .withDetail("waiting_threads", hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection())
                    .withDetail("url", hikariDataSource.getJdbcUrl());
        } else {
            builder.withDetail("database", "PostgreSQL")
                    .withDetail("driver", dataSource.getClass().getName());
        }

        return builder.build();
    }
}
