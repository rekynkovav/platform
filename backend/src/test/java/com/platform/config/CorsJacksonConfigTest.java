package com.platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CorsJacksonConfigTest {

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
