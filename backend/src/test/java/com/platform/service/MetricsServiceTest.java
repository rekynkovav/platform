package com.platform.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricsServiceTest {

    private MetricsService metricsService;
    private MeterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metricsService = new MetricsService(registry);
    }

    // ==================== Counter increments ====================

    @Test
    void incrementMessages_IncreasesCounter() {
        metricsService.incrementMessages();
        metricsService.incrementMessages();

        Counter counter = registry.find("chat.messages.total").counter();
        assertNotNull(counter);
        assertEquals(2.0, counter.count());
    }

    @Test
    void incrementBroadcasts_IncreasesCounter() {
        metricsService.incrementBroadcasts();

        Counter counter = registry.find("broadcast.total").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    void incrementErrors_IncreasesCounter() {
        metricsService.incrementErrors();
        metricsService.incrementErrors();
        metricsService.incrementErrors();

        Counter counter = registry.find("errors.total").counter();
        assertNotNull(counter);
        assertEquals(3.0, counter.count());
    }

    @Test
    void incrementLoginSuccess_IncreasesCounter() {
        metricsService.incrementLoginSuccess();

        Counter counter = registry.find("auth.login.success").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    void incrementLoginFailure_IncreasesCounter() {
        metricsService.incrementLoginFailure();
        metricsService.incrementLoginFailure();

        Counter counter = registry.find("auth.login.failure").counter();
        assertNotNull(counter);
        assertEquals(2.0, counter.count());
    }

    // ==================== WebSocket counters ====================

    @Test
    void incrementWebSocketConnections_IncreasesGauge() {
        metricsService.incrementWebSocketConnections();

        var gauge = registry.find("websocket.connections.active").gauge();
        assertNotNull(gauge);
        assertEquals(1.0, gauge.value());
    }

    @Test
    void decrementWebSocketConnections_DecreasesGauge() {
        metricsService.incrementWebSocketConnections();
        metricsService.incrementWebSocketConnections();
        metricsService.decrementWebSocketConnections();

        var gauge = registry.find("websocket.connections.active").gauge();
        assertNotNull(gauge);
        assertEquals(1.0, gauge.value());
    }

    @Test
    void incrementWebSocketErrors_IncreasesCounter() {
        metricsService.incrementWebSocketErrors();

        Counter counter = registry.find("websocket.errors.total").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    // ==================== Timers ====================

    @Test
    void startAndStopTimer_RecordsDuration() {
        Timer.Sample sample = metricsService.startTimer();
        assertNotNull(sample);

        metricsService.stopTimer(sample);

        Timer timer = registry.find("request.processing.time").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void stopTimer_NullSample_DoesNotThrow() {
        assertDoesNotThrow(() -> metricsService.stopTimer(null));
    }

    @Test
    void startAndStopWebSocketTimer_RecordsDuration() {
        Timer.Sample sample = metricsService.startWebSocketTimer();
        assertNotNull(sample);

        metricsService.stopWebSocketTimer(sample);

        Timer timer = registry.find("websocket.message.time").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void stopWebSocketTimer_NullSample_DoesNotThrow() {
        assertDoesNotThrow(() -> metricsService.stopWebSocketTimer(null));
    }

    // ==================== Scheduled rate updates ====================

    @Test
    void updateMessageRate_SetsMessagesPerSecond() {
        metricsService.incrementMessages();
        metricsService.incrementMessages();
        metricsService.incrementMessages();

        metricsService.updateMessageRate();

        // After first update, rate should reflect the delta
        // We can verify by checking gauge value
        var gauge = registry.find("chat.messages.rate").gauge();
        assertNotNull(gauge);
        assertEquals(3.0, gauge.value());
    }

    @Test
    void updateWebSocketConnectionsRate_SetsRate() {
        metricsService.incrementWebSocketConnections();
        metricsService.incrementWebSocketConnections();

        metricsService.updateWebSocketConnectionsRate();

        var gauge = registry.find("websocket.connections.rate").gauge();
        assertNotNull(gauge);
        assertEquals(2.0, gauge.value());
    }
}
