package com.example.riskmonitoring.producer.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ibm.mq.jms.MQQueueConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.retry.annotation.EnableRetry;

import javax.jms.ConnectionFactory;

/**
 * Configuration for JMS and IBM MQ setup.
 * Handles connection pooling, retry logic, and JSON serialization.
 */
@Slf4j
@Configuration
@EnableRetry
public class JmsConfiguration {

    /**
     * Creates and configures the IBM MQ Connection Factory bean.
     *
     * @param queueManager the queue manager name
     * @param channel the connection channel
     * @param connName the connection name (host and port)
     * @param user the user for MQ authentication
     * @param password the password for MQ authentication
     * @return configured MQQueueConnectionFactory
     */
    @Bean
    public MQQueueConnectionFactory mqQueueConnectionFactory(
            @Value("${spring.jms.ibm-mq.queue-manager}") String queueManager,
            @Value("${spring.jms.ibm-mq.channel}") String channel,
            @Value("${spring.jms.ibm-mq.connName}") String connName,
            @Value("${spring.jms.ibm-mq.user}") String user,
            @Value("${spring.jms.ibm-mq.password}") String password) throws Exception {

        MQQueueConnectionFactory factory = new MQQueueConnectionFactory();
        factory.setQueueManager(queueManager);
        factory.setChannel(channel);
        factory.setConnectionNameList(connName);
        factory.setUserID(user);
        factory.setPassword(password);
        factory.setTransportType(1); // TRANSPORT_MQSERIES = 1 (TCP/IP connection)

        log.info("IBM MQ Connection Factory configured: QM={}, Channel={}, ConnName={}",
                queueManager, channel, connName);

        return factory;
    }

    /**
     * Creates a caching connection factory wrapper with connection pooling.
     *
     * @param mqQueueConnectionFactory the IBM MQ connection factory
     * @return CachingConnectionFactory configured for connection pooling
     */
    @Bean
    public ConnectionFactory connectionFactory(MQQueueConnectionFactory mqQueueConnectionFactory) {
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(mqQueueConnectionFactory);
        
        // Connection pooling configuration
        cachingConnectionFactory.setSessionCacheSize(10);
        cachingConnectionFactory.setCacheConsumers(false);
        cachingConnectionFactory.setCacheProducers(true);
        
        // Reconnection settings
        cachingConnectionFactory.setExceptionListener(exception -> {
            log.error("JMS Connection exception occurred", exception);
        });
        
        log.info("Caching Connection Factory configured with session cache size: 10");
        
        return cachingConnectionFactory;
    }

    /**
     * Creates and configures the JmsTemplate bean for sending messages.
     * JmsTemplate handles connection and queue management internally.
     *
     * @param connectionFactory the connection factory
     * @return configured JmsTemplate
     */
    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
        
        // Use queue (not topic)
        jmsTemplate.setPubSubDomain(false);
        
        // Default destination unless overridden
        jmsTemplate.setDefaultDestinationName("TRANSACTION_QUEUE");
        
        // Timeout for send operations (5 seconds)
        jmsTemplate.setSendTimeout(5000L);
        
        // Explicitly receive timeout
        jmsTemplate.setReceiveTimeout(5000L);
        
        // Enable explicit QOS (Quality of Service)
        jmsTemplate.setExplicitQosEnabled(true);
        
        // Set delivery mode to PERSISTENT
        jmsTemplate.setDeliveryPersistent(true);
        
        // Set priority to 4 (default)
        jmsTemplate.setPriority(4);
        
        log.info("JMS Template configured with send timeout: 5000ms, persistent delivery enabled");
        
        return jmsTemplate;
    }

    /**
     * Creates and configures the ObjectMapper bean for JSON serialization.
     * Used for converting Transaction objects to JSON strings.
     *
     * @return configured ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Register Java time modules for Instant, LocalDateTime, etc.
        objectMapper.findAndRegisterModules();
        
        // Don't serialize dates as timestamps; use ISO format
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Don't fail on unknown properties during deserialization
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        
        log.info("ObjectMapper configured for JSON serialization with ISO date format");
        
        return objectMapper;
    }
}

