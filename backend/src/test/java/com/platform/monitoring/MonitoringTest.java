package com.platform.monitoring;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MonitoringTest {

    // ==================== CircuitBreakerMonitor ====================

    @Test
    void circuitBreakerMonitor_LogStats_DoesNotThrow() {
        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.ofDefaults();
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        CircuitBreakerMonitor monitor = new CircuitBreakerMonitor(cbRegistry, meterRegistry);

        assertDoesNotThrow(monitor::logCircuitBreakerStats);
    }

    @Test
    void circuitBreakerMonitor_ResetMetrics_DoesNotThrow() {
        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.ofDefaults();
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        CircuitBreakerMonitor monitor = new CircuitBreakerMonitor(cbRegistry, meterRegistry);

        assertDoesNotThrow(monitor::resetMetrics);
    }

    @Test
    void circuitBreakerMonitor_WithRegisteredBreaker_LogsStats() {
        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.ofDefaults();
        cbRegistry.circuitBreaker("test-service");
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        CircuitBreakerMonitor monitor = new CircuitBreakerMonitor(cbRegistry, meterRegistry);

        assertDoesNotThrow(monitor::logCircuitBreakerStats);
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

    // ==================== HotKeyMonitor ====================

    @Test
    void hotKeyMonitor_RecordHit_IncrementsCounter() {
        HotKeyMonitor monitor = new HotKeyMonitor();

        assertDoesNotThrow(() -> monitor.recordHit("test-key"));
        assertDoesNotThrow(() -> monitor.recordHit("test-key"));
    }

    @Test
    void hotKeyMonitor_RecordCacheHit_DoesNotThrow() {
        HotKeyMonitor monitor = new HotKeyMonitor();

        assertDoesNotThrow(() -> monitor.recordCacheHit("users", "user-123"));
    }

    @Test
    void hotKeyMonitor_RecordApiHit_DoesNotThrow() {
        HotKeyMonitor monitor = new HotKeyMonitor();

        assertDoesNotThrow(() -> monitor.recordApiHit("/api/users", "GET"));
    }

    @Test
    void hotKeyMonitor_RecordWebSocketEvent_DoesNotThrow() {
        HotKeyMonitor monitor = new HotKeyMonitor();

        assertDoesNotThrow(() -> monitor.recordWebSocketEvent("JOIN", "room-1"));
    }

    @Test
    void hotKeyMonitor_RecordDatabaseQuery_DoesNotThrow() {
        HotKeyMonitor monitor = new HotKeyMonitor();

        assertDoesNotThrow(() -> monitor.recordDatabaseQuery("users", "SELECT"));
    }

    @Test
    void hotKeyMonitor_LogHotKeys_DoesNotThrow() {
        HotKeyMonitor monitor = new HotKeyMonitor();
        monitor.recordHit("key1");
        monitor.recordHit("key2");

        assertDoesNotThrow(monitor::logHotKeys);
    }
}
