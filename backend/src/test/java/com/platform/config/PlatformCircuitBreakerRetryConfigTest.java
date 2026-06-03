package com.platform.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.retry.support.RetryTemplate;

import static org.junit.jupiter.api.Assertions.*;

class PlatformCircuitBreakerRetryConfigTest {

    // ==================== PlatformCircuitBreakerConfig ====================

    @Test
    void platformCB_CircuitBreakerRegistry_ReturnsNonNull() {
        PlatformCircuitBreakerConfig config = new PlatformCircuitBreakerConfig();
        CircuitBreakerRegistry registry = config.circuitBreakerRegistry();
        assertNotNull(registry);
    }

    @Test
    void platformCB_DatabaseCircuitBreaker_ReturnsCorrectName() {
        PlatformCircuitBreakerConfig config = new PlatformCircuitBreakerConfig();
        CircuitBreakerRegistry registry = config.circuitBreakerRegistry();
        CircuitBreaker cb = config.databaseCircuitBreaker(registry);

        assertNotNull(cb);
        assertEquals("database", cb.getName());
    }

    @Test
    void platformCB_ExternalApiCircuitBreaker_ReturnsCorrectName() {
        PlatformCircuitBreakerConfig config = new PlatformCircuitBreakerConfig();
        CircuitBreakerRegistry registry = config.circuitBreakerRegistry();
        CircuitBreaker cb = config.externalApiCircuitBreaker(registry);

        assertNotNull(cb);
        assertEquals("externalApi", cb.getName());
    }

    @Test
    void platformCB_Factory_ReturnsNonNull() {
        PlatformCircuitBreakerConfig config = new PlatformCircuitBreakerConfig();
        CircuitBreakerRegistry registry = config.circuitBreakerRegistry();
        Resilience4JCircuitBreakerFactory factory = config.circuitBreakerFactory(registry);

        assertNotNull(factory);
    }

    // ==================== RetryConfig ====================

    @Test
    void retryConfig_DatabaseRetryTemplate_ReturnsNonNull() {
        RetryConfig config = new RetryConfig();
        RetryTemplate template = config.databaseRetryTemplate();
        assertNotNull(template);
    }

    @Test
    void retryConfig_ExternalApiRetryTemplate_ReturnsNonNull() {
        RetryConfig config = new RetryConfig();
        RetryTemplate template = config.externalApiRetryTemplate();
        assertNotNull(template);
    }

    @Test
    void retryConfig_DatabaseRetryTemplate_HasMaxAttempts3() {
        RetryConfig config = new RetryConfig();
        RetryTemplate template = config.databaseRetryTemplate();

        // Execute with a counter to verify max attempts
        int[] attempts = {0};
        try {
            template.execute(context -> {
                attempts[0]++;
                throw new RuntimeException("test");
            });
        } catch (Exception ignored) {}

        assertEquals(3, attempts[0]);
    }

    @Test
    void retryConfig_ExternalApiRetryTemplate_HasMaxAttempts2() {
        RetryConfig config = new RetryConfig();
        RetryTemplate template = config.externalApiRetryTemplate();

        int[] attempts = {0};
        try {
            template.execute(context -> {
                attempts[0]++;
                throw new RuntimeException("test");
            });
        } catch (Exception ignored) {}

        assertEquals(2, attempts[0]);
    }
}
