package com.example.riskmonitoring.alertservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;

/**
 * JPA and Database configuration for alert-service.
 * Configures Spring Data JPA, transaction management, and entity scanning.
 */
@Slf4j
@Configuration
@EnableJpaRepositories(
        basePackages = "com.example.riskmonitoring.alertservice.repository",
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager"
)
@EnableTransactionManagement
public class DatabaseConfiguration {

    public DatabaseConfiguration() {
        log.info("Database configuration initialized for alert-service");
    }

    /**
     * Configures JPA transaction manager for reliable transaction handling.
     * Ensures ACID properties for database operations.
     *
     * @param entityManagerFactory the entity manager factory
     * @return configured transaction manager
     */
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        log.info("JPA Transaction Manager configured");
        return transactionManager;
    }
}

