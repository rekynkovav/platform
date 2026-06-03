package com.platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;

import static org.junit.jupiter.api.Assertions.*;

class ObjectMapperCircuitBreakerConfigTest {

    // ==================== ObjectMapperConfig ====================

    @Test
    void objectMapperConfig_ObjectMapper_IsNotNull() {
        ObjectMapperConfig config = new ObjectMapperConfig();
        ObjectMapper mapper = config.objectMapper();

        assertNotNull(mapper);
    }

    @Test
    void objectMapperConfig_ObjectMapper_HasJavaTimeModule() {
        ObjectMapperConfig config = new ObjectMapperConfig();
        ObjectMapper mapper = config.objectMapper();

        assertTrue(mapper.getRegisteredModuleIds().stream()
                .anyMatch(id -> id.toString().contains("JavaTimeModule") || id.toString().contains("jsr310")));
    }

    // ==================== PlatformCircuitBreakerConfig ====================

    @Test
    void circuitBreakerConfig_Registry_IsNotNull() {
        PlatformCircuitBreakerConfig config = new PlatformCircuitBreakerConfig();
        CircuitBreakerRegistry registry = config.circuitBreakerRegistry();

        assertNotNull(registry);
    }

    @Test
    void circuitBreakerConfig_DatabaseBreaker_HasCorrectName() {
        PlatformCircuitBreakerConfig config = new PlatformCircuitBreakerConfig();
        CircuitBreakerRegistry registry = config.circuitBreakerRegistry();

        CircuitBreaker breaker = config.databaseCircuitBreaker(registry);

        assertNotNull(breaker);
        assertEquals("database", breaker.getName());
    }

    @Test
    void circuitBreakerConfig_ExternalApiBreaker_HasCorrectName() {
        PlatformCircuitBreakerConfig config = new PlatformCircuitBreakerConfig();
        CircuitBreakerRegistry registry = config.circuitBreakerRegistry();

        CircuitBreaker breaker = config.externalApiCircuitBreaker(registry);

        assertNotNull(breaker);
        assertEquals("externalApi", breaker.getName());
    }

    @Test
    void circuitBreakerConfig_Factory_IsNotNull() {
        PlatformCircuitBreakerConfig config = new PlatformCircuitBreakerConfig();
        CircuitBreakerRegistry registry = config.circuitBreakerRegistry();

        Resilience4JCircuitBreakerFactory factory = config.circuitBreakerFactory(registry);

        assertNotNull(factory);
    }
}
