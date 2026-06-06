package com.platform.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricsObjectMapperConfigTest {

    // ==================== JacksonConfig ====================

    @Test
    void jacksonConfig_ObjectMapper_ReturnsNonNull() {
        JacksonConfig config = new JacksonConfig();
        ObjectMapper mapper = config.objectMapper();

        assertNotNull(mapper);
    }

    @Test
    void jacksonConfig_ObjectMapper_EnablesFailOnUnknownProperties() {
        JacksonConfig config = new JacksonConfig();
        ObjectMapper mapper = config.objectMapper();

        assertTrue(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }

    @Test
    void jacksonConfig_ObjectMapper_DisablesFailOnInvalidSubtype() {
        JacksonConfig config = new JacksonConfig();
        ObjectMapper mapper = config.objectMapper();

        assertFalse(mapper.isEnabled(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE));
    }

    @Test
    void jacksonConfig_ObjectMapper_HasJavaTimeModule() throws Exception {
        JacksonConfig config = new JacksonConfig();
        ObjectMapper mapper = config.objectMapper();

        // Verify JavaTimeModule is registered by serializing LocalDateTime
        java.time.LocalDateTime now = java.time.LocalDateTime.of(2026, 6, 1, 10, 30);
        String json = mapper.writeValueAsString(now);

        assertNotNull(json);
        assertTrue(json.contains("2026"));
    }
}
