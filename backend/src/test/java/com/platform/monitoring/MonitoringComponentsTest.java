package com.platform.monitoring;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MonitoringComponentsTest {

    // ==================== CircuitBreakerMonitor ====================

    @Test
    void circuitBreakerMonitor_LogStats_DoesNotThrow() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        MeterRegistry meterRegistry = new SimpleMeterRegistry();

        CircuitBreakerMonitor monitor = new CircuitBreakerMonitor(registry, meterRegistry);

        assertDoesNotThrow(monitor::logCircuitBreakerStats);
    }

    @Test
    void circuitBreakerMonitor_ResetMetrics_DoesNotThrow() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        MeterRegistry meterRegistry = new SimpleMeterRegistry();

        CircuitBreakerMonitor monitor = new CircuitBreakerMonitor(registry, meterRegistry);

        assertDoesNotThrow(monitor::resetMetrics);
    }

    @Test
    void circuitBreakerMonitor_WithOpenBreaker_LogsWarning() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker cb = registry.circuitBreaker("testService");
        cb.transitionToOpenState();

        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        CircuitBreakerMonitor monitor = new CircuitBreakerMonitor(registry, meterRegistry);

        assertDoesNotThrow(monitor::logCircuitBreakerStats);
    }

    // ==================== HotKeyMonitor ====================

    @Test
    void hotKeyMonitor_RecordHit_IncrementsCounter() {
        HotKeyMonitor monitor = new HotKeyMonitor();

        monitor.recordHit("test-key");
        monitor.recordHit("test-key");
        monitor.recordHit("test-key");

        assertDoesNotThrow(monitor::logHotKeys);
    }

    @Test
    void hotKeyMonitor_RecordCacheHit_Works() {
        HotKeyMonitor monitor = new HotKeyMonitor();
        assertDoesNotThrow(() -> monitor.recordCacheHit("users", "user-1"));
    }

    @Test
    void hotKeyMonitor_RecordApiHit_Works() {
        HotKeyMonitor monitor = new HotKeyMonitor();
        assertDoesNotThrow(() -> monitor.recordApiHit("/api/users", "GET"));
    }

    @Test
    void hotKeyMonitor_RecordWebSocketEvent_Works() {
        HotKeyMonitor monitor = new HotKeyMonitor();
        assertDoesNotThrow(() -> monitor.recordWebSocketEvent("MESSAGE", "room-1"));
    }

    @Test
    void hotKeyMonitor_RecordDatabaseQuery_Works() {
        HotKeyMonitor monitor = new HotKeyMonitor();
        assertDoesNotThrow(() -> monitor.recordDatabaseQuery("users", "SELECT"));
    }

    // ==================== MemoryMonitor ====================

    @Test
    void memoryMonitor_GetMemoryReport_ReturnsValidString() {
        MemoryMonitor monitor = new MemoryMonitor();
        String report = monitor.getMemoryReport();

        assertNotNull(report);
        assertTrue(report.contains("Heap:"));
        assertTrue(report.contains("NonHeap:"));
        assertTrue(report.contains("GC count:"));
    }

    @Test
    void memoryMonitor_LogMemoryUsage_DoesNotThrow() {
        MemoryMonitor monitor = new MemoryMonitor();
        assertDoesNotThrow(monitor::logMemoryUsage);
    }
}
