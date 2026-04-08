package com.example.riskmonitoring.alertservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

/**
 * Web and pagination configuration for REST endpoints.
 */
@Slf4j
@Configuration
@EnableSpringDataWebSupport
public class WebConfiguration {

    public WebConfiguration() {
        log.info("Web configuration initialized with Spring Data Web Support");
    }
}
