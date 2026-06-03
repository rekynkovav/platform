package com.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

class RetrySecurityConfigTest {

    // ==================== RetryConfig ====================

    @Test
    void retryConfig_DatabaseRetryTemplate_IsNotNull() {
        RetryConfig config = new RetryConfig();
        RetryTemplate template = config.databaseRetryTemplate();

        assertNotNull(template);
    }

    @Test
    void retryConfig_ExternalApiRetryTemplate_IsNotNull() {
        RetryConfig config = new RetryConfig();
        RetryTemplate template = config.externalApiRetryTemplate();

        assertNotNull(template);
    }

    // ==================== SecurityConfig ====================

    @Test
    void securityConfig_JwtAuthenticationConverter_IsNotNull() {
        SecurityConfig config = new SecurityConfig();
        JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();

        assertNotNull(converter);
    }

    @Test
    void securityConfig_CorsConfigurationSource_IsNotNull() {
        SecurityConfig config = new SecurityConfig();
        // Set allowedOrigins via reflection since it's @Value injected
        org.springframework.test.util.ReflectionTestUtils.setField(config, "allowedOrigins",
                java.util.List.of("http://localhost:3000"));

        CorsConfigurationSource source = config.corsConfigurationSource();

        assertNotNull(source);
    }
}
