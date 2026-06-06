package com.platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;

import static org.junit.jupiter.api.Assertions.*;

class ObjectMapperCircuitBreakerConfigTest {

    // ==================== JacksonConfig (replaces deleted ObjectMapperConfig) ====================

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
