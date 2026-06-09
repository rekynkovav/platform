package com.platform.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MetricsService {

    private final Counter messageCounter;
    private final Counter broadcastCounter;
    private final Counter errorCounter;
    private final Timer processingTimer;
    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final AtomicInteger activeWebSocketConnections = new AtomicInteger(0);
    private final Timer websocketMessageTimer;
    private final Counter websocketErrorsCounter;

    private final AtomicLong messagesPerSecond = new AtomicLong(0);
    private final AtomicLong websocketConnectionsPerSecond = new AtomicLong(0);
    private long lastMessageCount = 0;
    private long lastWebSocketConnectionsCount = 0;

    public MetricsService(MeterRegistry registry) {
        this.messageCounter = Counter.builder("chat.messages.total")
            .description("Total number of chat messages")
            .tag("type", "all")
            .register(registry);

        this.broadcastCounter = Counter.builder("broadcast.total")
            .description("Total number of broadcasts started")
            .register(registry);

        this.errorCounter = Counter.builder("errors.total")
            .description("Total number of errors")
            .register(registry);

        this.processingTimer = Timer.builder("request.processing.time")
            .description("Request processing time in seconds")
            .register(registry);

        this.loginSuccessCounter = Counter.builder("auth.login.success")
            .description("Successful logins")
            .register(registry);

        this.loginFailureCounter = Counter.builder("auth.login.failure")
            .description("Failed logins")
            .register(registry);

        Gauge.builder("websocket.connections.active", activeWebSocketConnections, AtomicInteger::get)
            .description("Active WebSocket connections")
            .register(registry);

        this.websocketMessageTimer = Timer.builder("websocket.message.time")
            .description("WebSocket message processing time")
            .register(registry);

        this.websocketErrorsCounter = Counter.builder("websocket.errors.total")
            .description("Total WebSocket errors")
            .register(registry);

        // Rate metrics
        Gauge.builder("chat.messages.rate", messagesPerSecond, AtomicLong::get)
            .description("Messages per second")
            .register(registry);

        Gauge.builder("websocket.connections.rate", websocketConnectionsPerSecond, AtomicLong::get)
            .description("WebSocket connections per second")
            .register(registry);
    }

    public void incrementMessages() { messageCounter.increment(); }
    public void incrementBroadcasts() { broadcastCounter.increment(); }
    public void incrementErrors() { errorCounter.increment(); }
    public void incrementLoginSuccess() { loginSuccessCounter.increment(); }
    public void incrementLoginFailure() { loginFailureCounter.increment(); }

    public void incrementWebSocketConnections() {
        activeWebSocketConnections.incrementAndGet();
    }

    public void decrementWebSocketConnections() {
        activeWebSocketConnections.decrementAndGet();
    }

    public Timer.Sample startWebSocketTimer() {
        return Timer.start();
    }

    public void stopWebSocketTimer(Timer.Sample sample) {
        if (sample != null) sample.stop(websocketMessageTimer);
    }

    public void incrementWebSocketErrors() {
        websocketErrorsCounter.increment();
    }

    public Timer.Sample startTimer() { return Timer.start(); }

    public void stopTimer(Timer.Sample sample) {
        if (sample != null) sample.stop(processingTimer);
    }

    @Scheduled(fixedRate = 1000)
    public void updateMessageRate() {
        long current = (long) messageCounter.count();
        long delta = current - lastMessageCount;
        lastMessageCount = current;
        messagesPerSecond.set(delta);
    }

    @Scheduled(fixedRate = 1000)
    public void updateWebSocketConnectionsRate() {
        long current = activeWebSocketConnections.get();
        long delta = current - lastWebSocketConnectionsCount;
        lastWebSocketConnectionsCount = current;
        websocketConnectionsPerSecond.set(delta);
    }
}