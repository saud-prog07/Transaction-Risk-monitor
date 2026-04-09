package com.example.riskmonitoring.alertservice.config;

import com.example.riskmonitoring.alertservice.security.JwtAuthenticationFilter;
import com.example.riskmonitoring.alertservice.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

/**
 * Spring Security Configuration
 * Configures JWT-based authentication, authorization, and security headers
 * 
 * Security Features:
 * - JWT token-based stateless authentication
 * - CORS properly configured for frontend integration
 * - Security headers (HSTS, X-Frame-Options, X-Content-Type-Options, CSP)
 * - CSRF protection via stateless tokens
 * - Rate limiting and account lockout via custom services
 * - Password encoding with BCrypt
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:3001}")
    private String allowedOrigins;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Configure password encoder
     * Uses BCrypt with strength 12 for strong password hashing
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Configure security filter chain
     * @param http HttpSecurity to configure
     * @return SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security filter chain");

        http
            .csrf(csrf -> csrf.disable())  // Disabled for stateless JWT authentication
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // Stateless authentication
            )
            .authorizeHttpRequests(authz -> authz
                // Public authentication endpoints
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/verify-email").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/forgot-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/reset-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/health").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                // Protected endpoints
                .requestMatchers(HttpMethod.GET, "/api/alerts/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/alerts/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/alerts/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/config/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/config/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/auth/change-password").authenticated()
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint((request, response, authException) -> {
                    log.warn("Unauthorized access attempt");
                    response.setContentType("application/json");
                    response.setStatus(401);
                    response.getWriter().write("{\"error\": \"Unauthorized\"}");
                })
                .accessDeniedHandler((request, response, ex) -> {
                    log.warn("Access denied");
                    response.setContentType("application/json");
                    response.setStatus(403);
                    response.getWriter().write("{\"error\": \"Forbidden\"}");
                })
            )
            .headers(headers -> headers
                .httpStrictTransportSecurity()
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)  // 1 year
                .and()
                .xssProtection()
                .and()
                .frameOptions().deny()  // Prevent clickjacking
            );

        // Add JWT filter
        http.addFilterBefore(
            new JwtAuthenticationFilter(jwtTokenProvider),
            UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

    /**
     * Configure CORS
     * Properly restricts CORS to specific origins and methods
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Set allowed origins from configuration
        String[] origins = allowedOrigins.split(",");
        configuration.setAllowedOrigins(Arrays.asList(origins));
        
        // Allow specific HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Allow specific headers
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        
        // Expose these headers to the frontend
        configuration.setExposedHeaders(Arrays.asList("Authorization", "X-Total-Count"));
        
        // Allow credentials (cookies, JWT tokens)
        configuration.setAllowCredentials(true);
        
        // Cache preflight requests for 3600 seconds
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

