package com.platform.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResilienceServiceTest {

    private ResilienceService resilienceService;
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp() {
        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
        BulkheadRegistry bulkheadRegistry = BulkheadRegistry.ofDefaults();

        resilienceService = new ResilienceService(
                circuitBreakerRegistry, retryRegistry, timeLimiterRegistry, bulkheadRegistry);
    }

    // ==================== executeWithCircuitBreaker ====================

    @Test
    void executeWithCircuitBreaker_Success_ReturnsValue() {
        String result = resilienceService.executeWithCircuitBreaker("test-cb", () -> "ok");
        assertEquals("ok", result);
    }

    @Test
    void executeWithCircuitBreaker_Exception_PropagatesWhenClosed() {
        assertThrows(RuntimeException.class, () ->
                resilienceService.executeWithCircuitBreaker("test-cb-err", () -> {
                    throw new RuntimeException("fail");
                }));
    }

    // ==================== executeWithRetry ====================

    @Test
    void executeWithRetry_Success_ReturnsValue() {
        String result = resilienceService.executeWithRetry("test-retry", () -> "retried");
        assertEquals("retried", result);
    }

    // ==================== executeWithBulkhead ====================

    @Test
    void executeWithBulkhead_Success_ReturnsValue() {
        String result = resilienceService.executeWithBulkhead("test-bh", () -> "bulkheaded");
        assertEquals("bulkheaded", result);
    }

    // ==================== getCircuitBreakerState ====================

    @Test
    void getCircuitBreakerState_NewBreaker_IsClosed() {
        CircuitBreaker.State state = resilienceService.getCircuitBreakerState("new-cb");
        assertEquals(CircuitBreaker.State.CLOSED, state);
    }

    // ==================== resetCircuitBreaker ====================

    @Test
    void resetCircuitBreaker_DoesNotThrow() {
        assertDoesNotThrow(() -> resilienceService.resetCircuitBreaker("reset-cb"));
    }

    // ==================== getCircuitBreakerMetrics ====================

    @Test
    void getCircuitBreakerMetrics_ReturnsNonNull() {
        var metrics = resilienceService.getCircuitBreakerMetrics("metrics-cb");
        assertNotNull(metrics);
        assertEquals(0, metrics.getNumberOfBufferedCalls());
    }

    // ==================== isServiceAvailable ====================

    @Test
    void isServiceAvailable_ClosedBreaker_ReturnsTrue() {
        assertTrue(resilienceService.isServiceAvailable("avail-cb"));
    }

    // ==================== executeWithFallback ====================

    @Test
    void executeWithFallback_PrimarySucceeds_ReturnsPrimary() {
        String result = resilienceService.executeWithFallback(
                "fb-cb", () -> "primary", () -> "fallback");
        assertEquals("primary", result);
    }

    @Test
    void executeWithFallback_PrimaryFails_ReturnsFallback() {
        String result = resilienceService.executeWithFallback(
                "fb-cb-fail",
                () -> { throw new RuntimeException("boom"); },
                () -> "fallback-value");
        assertEquals("fallback-value", result);
    }

    // ==================== logCircuitBreakerStates ====================

    @Test
    void logCircuitBreakerStates_DoesNotThrow() {
        assertDoesNotThrow(resilienceService::logCircuitBreakerStates);
    }

    // ==================== executeWithResilience (CB + Retry) ====================

    @Test
    void executeWithResilience_Success_ReturnsValue() {
        String result = resilienceService.executeWithResilience("res-cb", "res-retry", () -> "resilient");
        assertEquals("resilient", result);
    }
}
