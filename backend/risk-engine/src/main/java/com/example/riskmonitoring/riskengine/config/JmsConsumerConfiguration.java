package com.example.riskmonitoring.riskengine.config;

import com.ibm.mq.jms.MQQueueConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.client.RestTemplate;

import javax.jms.ConnectionFactory;

/**
 * Configuration for JMS consumer and HTTP client setup.
 */
@Slf4j
@Configuration
@EnableJms
public class JmsConsumerConfiguration {

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
            @Value("${spring.jms.ibm-mq.password}") String password) {

        try {
            MQQueueConnectionFactory factory = new MQQueueConnectionFactory();
            factory.setQueueManager(queueManager);
            factory.setChannel(channel);
            factory.setConnectionNameList(connName);
            factory.setUserID(user);
            factory.setPassword(password);
            factory.setTransportType(1); // TCP/IP connection

            log.info("IBM MQ Connection Factory configured: QM={}, Channel={}, ConnName={}",
                    queueManager, channel, connName);

            return factory;
        } catch (Exception ex) {
            log.error("Error configuring IBM MQ Connection Factory", ex);
            throw new RuntimeException("Failed to configure IBM MQ connection", ex);
        }
    }

    /**
     * Creates a caching connection factory wrapper.
     *
     * @param mqQueueConnectionFactory the IBM MQ connection factory
     * @return CachingConnectionFactory configured for connection pooling
     */
    @Bean
    public ConnectionFactory connectionFactory(MQQueueConnectionFactory mqQueueConnectionFactory) {
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(mqQueueConnectionFactory);
        cachingConnectionFactory.setSessionCacheSize(10);
        cachingConnectionFactory.setCacheConsumers(true);
        cachingConnectionFactory.setCacheProducers(false);

        log.info("Caching Connection Factory configured with session cache size: 10");

        return cachingConnectionFactory;
    }

    /**
     * Creates and configures the JMS listener container factory.
     * Used by @JmsListener annotation.
     *
     * @param connectionFactory the connection factory
     * @return DefaultJmsListenerContainerFactory
     */
    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            ConnectionFactory connectionFactory,
            @Value("${jms.listener.concurrency:1-10}") String concurrency) {

        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrency(concurrency);
        factory.setSessionTransacted(true); // Enable JTA transactions

        // Error handling
        factory.setErrorHandler(throwable -> {
            log.error("Error in JMS listener", throwable);
        });

        log.info("JMS Listener Container Factory configured with concurrency: {}", concurrency);

        return factory;
    }

    /**
     * Creates and configures JmsTemplate bean (if needed for sending messages).
     *
     * @param connectionFactory the connection factory
     * @return configured JmsTemplate
     */
    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
        jmsTemplate.setPubSubDomain(false);
        jmsTemplate.setSessionTransacted(true);
        return jmsTemplate;
    }

    /**
     * RestTemplate bean for HTTP communication with alert-service.
     *
     * @return RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
