package com.example.riskmonitoring.alertservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web and pagination configuration for REST endpoints.
 * Also registers rate limiting and abuse detection interceptor
 */
@Slf4j
@Configuration
@EnableSpringDataWebSupport
public class WebConfiguration implements WebMvcConfigurer {

    private final RateLimitingInterceptor rateLimitingInterceptor;

    public WebConfiguration(RateLimitingInterceptor rateLimitingInterceptor) {
        this.rateLimitingInterceptor = rateLimitingInterceptor;
        log.info("Web configuration initialized with Spring Data Web Support");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/health", "/metrics", "/actuator/**");
        log.info("Rate limiting interceptor registered");
    }
}
