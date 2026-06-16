package com.platform.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResilienceConfigTest {

    private final ResilienceConfig config = new ResilienceConfig();

    @Test
    void keycloakCircuitBreaker_HasCorrectName() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker cb = config.keycloakCircuitBreaker(registry);
        assertNotNull(cb);
        assertEquals("keycloak", cb.getName());
    }

    @Test
    void keycloakRetry_HasCorrectName() {
        Retry retry = config.keycloakRetry();
        assertNotNull(retry);
        assertEquals("keycloak", retry.getName());
    }

    @Test
    void externalApiRetry_HasCorrectName() {
        Retry retry = config.externalApiRetry();
        assertNotNull(retry);
        assertEquals("external-api", retry.getName());
    }

    @Test
    void slowOperationsTimeLimiter_HasCorrectName() {
        TimeLimiter tl = config.slowOperationsTimeLimiter();
        assertNotNull(tl);
        assertEquals("slow-operations", tl.getName());
    }

    @Test
    void keycloakBulkhead_HasCorrectName() {
        Bulkhead bh = config.keycloakBulkhead();
        assertNotNull(bh);
        assertEquals("keycloak", bh.getName());
    }

    @Test
    void databaseBulkhead_HasCorrectName() {
        Bulkhead bh = config.databaseBulkhead();
        assertNotNull(bh);
        assertEquals("database", bh.getName());
    }

    @Test
    void registries_AreNotNull() {
        // circuitBreakerRegistry moved to PlatformCircuitBreakerConfig
        assertNotNull(config.retryRegistry());
        assertNotNull(config.timeLimiterRegistry());
        assertNotNull(config.bulkheadRegistry());
    }
}
