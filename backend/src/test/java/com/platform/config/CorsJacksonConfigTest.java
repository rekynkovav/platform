package com.platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CorsJacksonConfigTest {

    // ==================== CorsConfig ====================

    @Test
    void corsConfig_AddCorsMappings_RegistersApiMapping() {
        CorsConfig config = new CorsConfig();
        CorsRegistry registry = mock(CorsRegistry.class);
        CorsRegistration registration = mock(CorsRegistration.class);

        org.mockito.Mockito.when(registry.addMapping("/api/**")).thenReturn(registration);
        org.mockito.Mockito.when(registration.allowedOrigins("http://localhost:3000", "https://platform.local")).thenReturn(registration);
        org.mockito.Mockito.when(registration.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")).thenReturn(registration);
        org.mockito.Mockito.when(registration.allowedHeaders("*")).thenReturn(registration);
        org.mockito.Mockito.when(registration.allowCredentials(true)).thenReturn(registration);
        org.mockito.Mockito.when(registration.maxAge(3600)).thenReturn(registration);

        config.addCorsMappings(registry);

        verify(registry).addMapping("/api/**");
    }

    // ==================== JacksonConfig ====================

    @Test
    void jacksonConfig_ObjectMapper_IsNotNull() {
        JacksonConfig config = new JacksonConfig();
        ObjectMapper mapper = config.objectMapper();

        assertNotNull(mapper);
    }

    @Test
    void jacksonConfig_ObjectMapper_HasJavaTimeModule() {
        JacksonConfig config = new JacksonConfig();
        ObjectMapper mapper = config.objectMapper();

        // Verify that JavaTimeModule is registered by checking serialization of LocalDateTime
        assertNotNull(mapper.getRegisteredModuleIds());
        assertTrue(mapper.getRegisteredModuleIds().stream()
                .anyMatch(id -> String.valueOf(id).contains("JavaTimeModule") || String.valueOf(id).contains("jsr310")));
    }

    @Test
    void jacksonConfig_ObjectMapperBuilder_IsNotNull() {
        JacksonConfig config = new JacksonConfig();
        Jackson2ObjectMapperBuilder builder = config.objectMapperBuilder();

        assertNotNull(builder);
    }
}
