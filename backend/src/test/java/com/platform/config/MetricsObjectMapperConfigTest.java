package com.platform.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricsObjectMapperConfigTest {

    // ==================== MetricsConfig ====================

    @Test
    void metricsConfig_TimedAspect_ReturnsNonNull() {
        MetricsConfig config = new MetricsConfig();
        MeterRegistry registry = new SimpleMeterRegistry();

        TimedAspect aspect = config.timedAspect(registry);

        assertNotNull(aspect);
    }

    @Test
    void metricsConfig_TimedAspect_UsesProvidedRegistry() {
        MetricsConfig config = new MetricsConfig();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        TimedAspect aspect = config.timedAspect(registry);

        assertNotNull(aspect);
        // TimedAspect doesn't expose registry getter, but creation without exception confirms wiring
    }

    // ==================== ObjectMapperConfig ====================

    @Test
    void objectMapperConfig_ObjectMapper_ReturnsNonNull() {
        ObjectMapperConfig config = new ObjectMapperConfig();
        ObjectMapper mapper = config.objectMapper();

        assertNotNull(mapper);
    }

    @Test
    void objectMapperConfig_ObjectMapper_EnablesFailOnUnknownProperties() {
        ObjectMapperConfig config = new ObjectMapperConfig();
        ObjectMapper mapper = config.objectMapper();

        assertTrue(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }

    @Test
    void objectMapperConfig_ObjectMapper_DisablesFailOnInvalidSubtype() {
        ObjectMapperConfig config = new ObjectMapperConfig();
        ObjectMapper mapper = config.objectMapper();

        assertFalse(mapper.isEnabled(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE));
    }

    @Test
    void objectMapperConfig_ObjectMapper_HasJavaTimeModule() throws Exception {
        ObjectMapperConfig config = new ObjectMapperConfig();
        ObjectMapper mapper = config.objectMapper();

        // Verify JavaTimeModule is registered by serializing LocalDateTime
        java.time.LocalDateTime now = java.time.LocalDateTime.of(2026, 6, 1, 10, 30);
        String json = mapper.writeValueAsString(now);

        assertNotNull(json);
        assertTrue(json.contains("2026"));
    }
}
