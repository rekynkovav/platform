package com.platform.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResilienceService {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;
    private final BulkheadRegistry bulkheadRegistry;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    // Метод для выполнения с Circuit Breaker
    public <T> T executeWithCircuitBreaker(String circuitBreakerName, Supplier<T> supplier) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
        return circuitBreaker.executeSupplier(supplier);
    }

    // Метод для выполнения с Retry
    public <T> T executeWithRetry(String retryName, Supplier<T> supplier) {
        Retry retry = retryRegistry.retry(retryName);
        return retry.executeSupplier(supplier);
    }

    // Метод для выполнения с Circuit Breaker и Retry
    public <T> T executeWithResilience(String circuitBreakerName, String retryName, Supplier<T> supplier) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
        Retry retry = retryRegistry.retry(retryName);

        Supplier<T> decoratedSupplier = Retry.decorateSupplier(retry, supplier);
        return circuitBreaker.executeSupplier(decoratedSupplier);
    }

    // Метод для выполнения с Time Limiter
    public <T> CompletableFuture<T> executeWithTimeLimiter(String timeLimiterName, Callable<T> callable) {
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter(timeLimiterName);
        return timeLimiter.executeCompletionStage(
            scheduler,
            () -> CompletableFuture.supplyAsync(() -> {
                try {
                    return callable.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
        ).toCompletableFuture();
    }

    // Метод для выполнения с Bulkhead
    public <T> T executeWithBulkhead(String bulkheadName, Supplier<T> supplier) {
        Bulkhead bulkhead = bulkheadRegistry.bulkhead(bulkheadName);
        return bulkhead.executeSupplier(supplier);
    }

    // Комплексный метод со всеми паттернами
    public <T> T executeWithAllResiliencePatterns(
            String circuitBreakerName,
            String retryName,
            String bulkheadName,
            Supplier<T> supplier) {

        Bulkhead bulkhead = bulkheadRegistry.bulkhead(bulkheadName);
        Retry retry = retryRegistry.retry(retryName);
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);

        Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);
        decoratedSupplier = Retry.decorateSupplier(retry, decoratedSupplier);
        decoratedSupplier = Bulkhead.decorateSupplier(bulkhead, decoratedSupplier);

        return decoratedSupplier.get();
    }

    // Метод для получения статуса Circuit Breaker
    public CircuitBreaker.State getCircuitBreakerState(String circuitBreakerName) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
        return circuitBreaker.getState();
    }

    // Метод для сброса Circuit Breaker
    public void resetCircuitBreaker(String circuitBreakerName) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
        circuitBreaker.reset();
        log.info("Circuit breaker {} has been reset", circuitBreakerName);
    }

    // Метод для получения метрик
    public CircuitBreaker.Metrics getCircuitBreakerMetrics(String circuitBreakerName) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
        return circuitBreaker.getMetrics();
    }

    // Метод для логирования состояния всех Circuit Breaker
    public void logCircuitBreakerStates() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(circuitBreaker -> {
            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
            log.info("Circuit Breaker {}: State={}, FailureRate={}%, Calls={}, NotPermitted={}",
                circuitBreaker.getName(),
                circuitBreaker.getState(),
                metrics.getFailureRate(),
                metrics.getNumberOfBufferedCalls(),
                metrics.getNumberOfNotPermittedCalls()
            );
        });
    }

    // Метод для проверки доступности сервиса
    public boolean isServiceAvailable(String circuitBreakerName) {
        try {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
            return circuitBreaker.getState() != CircuitBreaker.State.OPEN;
        } catch (Exception e) {
            log.warn("Error checking circuit breaker state for {}", circuitBreakerName, e);
            return false;
        }
    }

    // Метод для graceful fallback
    public <T> T executeWithFallback(String circuitBreakerName, Supplier<T> supplier, Supplier<T> fallback) {
        try {
            return executeWithCircuitBreaker(circuitBreakerName, supplier);
        } catch (Exception e) {
            log.warn("Primary execution failed, using fallback for {}", circuitBreakerName, e);
            return fallback.get();
        }
    }
}