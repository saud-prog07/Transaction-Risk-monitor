package com.example.riskmonitoring.riskengine.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for HTTP client setup.
 * JMS has been DISABLED due to IBM MQ 9.3.4.1 incompatibility with Spring Boot 3.4's jakarta.jms
 * 
 * Services communicate via REST APIs instead of JMS.
 */
@Slf4j
@Configuration
@EnableScheduling
public class JmsConsumerConfiguration {

    /**
     * RestTemplate bean for HTTP communication with other services.
     *
     * @return RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
