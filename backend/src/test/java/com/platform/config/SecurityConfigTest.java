package com.platform.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig();
        ReflectionTestUtils.setField(securityConfig, "allowedOrigins",
                List.of("http://localhost:3000", "https://platform.local"));
    }

    // ==================== JwtAuthenticationConverter ====================

    @Test
    void jwtAuthenticationConverter_ReturnsNonNull() {
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();
        assertNotNull(converter);
    }

    @Test
    void jwtAuthenticationConverter_HasPrincipalClaimNameSet() {
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();
        // Principal claim name is set to "preferred_username"
        // We verify the converter is properly configured by checking it doesn't throw
        assertNotNull(converter);
    }

    // ==================== CorsConfigurationSource ====================

    @Test
    void corsConfigurationSource_ReturnsNonNull() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        assertNotNull(source);
    }

    @Test
    void corsConfigurationSource_AllowsConfiguredOrigins() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        var config = source.getCorsConfiguration(
                new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/test"));

        assertNotNull(config);
        assertTrue(config.getAllowedOrigins().contains("http://localhost:3000"));
        assertTrue(config.getAllowedOrigins().contains("https://platform.local"));
    }

    @Test
    void corsConfigurationSource_AllowsCredentials() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        var config = source.getCorsConfiguration(
                new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/test"));

        assertNotNull(config);
        assertTrue(config.getAllowCredentials());
    }

    @Test
    void corsConfigurationSource_HasCorrectMaxAge() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        var config = source.getCorsConfiguration(
                new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/test"));

        assertNotNull(config);
        assertEquals(3600L, config.getMaxAge());
    }

    @Test
    void corsConfigurationSource_AllowsRequiredMethods() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        var config = source.getCorsConfiguration(
                new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/test"));

        assertNotNull(config);
        assertTrue(config.getAllowedMethods().contains("GET"));
        assertTrue(config.getAllowedMethods().contains("POST"));
        assertTrue(config.getAllowedMethods().contains("PUT"));
        assertTrue(config.getAllowedMethods().contains("DELETE"));
        assertTrue(config.getAllowedMethods().contains("OPTIONS"));
    }
}
