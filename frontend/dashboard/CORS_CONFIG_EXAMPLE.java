// ====================================================================== 
// File: alert-service/src/main/java/com/example/riskmonitoring/alertservice/config/CorsConfig.java
// ======================================================================
// If you get a CORS error, add this configuration to your Alert Service

package com.example.riskmonitoring.alertservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS Configuration for React Dashboard
 * Allows requests from localhost:3000 (or your React app URL)
 */
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Development
                registry.addMapping("/api/**")
                    .allowedOrigins(
                        "http://localhost:3000",      // Local React dev
                        "http://localhost:8080",      // Alt port
                        "http://127.0.0.1:3000"       // Loopback address
                    )
                    .allowedMethods("GET", "PUT", "POST", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(true)
                    .maxAge(3600);

                // Actuator endpoints (health checks)
                registry.addMapping("/api/actuator/**")
                    .allowedOrigins("*")
                    .allowedMethods("GET")
                    .maxAge(3600);
            }
        };
    }
}

// ======================================================================
// Production Configuration (if needed):
// ======================================================================
/*
@Configuration
public class CorsConfig {

    @Value("${dashboard.url:http://localhost:3000}")
    private String dashboardUrl;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins(dashboardUrl)
                    .allowedMethods("GET", "PUT", "POST", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(true)
                    .maxAge(3600);
            }
        };
    }
}
*/

// ======================================================================
// Then add to application.yml:
// ======================================================================
/*
# CORS Configuration (Production)
dashboard:
  url: https://dashboard.yourdomain.com
*/
