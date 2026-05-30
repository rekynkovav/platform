package com.platform.monitoring;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class CircuitBreakerMonitor {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final MeterRegistry meterRegistry;

    public CircuitBreakerMonitor(CircuitBreakerRegistry circuitBreakerRegistry, MeterRegistry meterRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedRate = 10000) // каждые 10 секунд
    public void logCircuitBreakerStats() {
        Set<CircuitBreaker> circuitBreakers = circuitBreakerRegistry.getAllCircuitBreakers();

        for (CircuitBreaker circuitBreaker : circuitBreakers) {
            String name = circuitBreaker.getName();

            CircuitBreaker.State state = circuitBreaker.getState();
            float failureRate = circuitBreaker.getMetrics().getFailureRate();
            int bufferedCalls = circuitBreaker.getMetrics().getNumberOfBufferedCalls();
            int failedCalls = circuitBreaker.getMetrics().getNumberOfFailedCalls();
            int successfulCalls = circuitBreaker.getMetrics().getNumberOfSuccessfulCalls();

            log.info("Circuit Breaker '{}': state={}, failureRate={}%, calls={} (success={}, failed={})",
                name, state, failureRate * 100, bufferedCalls, successfulCalls, failedCalls);

            // Экспорт метрик в Micrometer
            meterRegistry.gauge("circuitbreaker." + name + ".state", state.ordinal());
            meterRegistry.gauge("circuitbreaker." + name + ".failure_rate", failureRate);
            meterRegistry.gauge("circuitbreaker." + name + ".buffered_calls", bufferedCalls);
            meterRegistry.gauge("circuitbreaker." + name + ".failed_calls", failedCalls);
            meterRegistry.gauge("circuitbreaker." + name + ".successful_calls", successfulCalls);

            // Логирование при переходе в OPEN состояние
            if (state == CircuitBreaker.State.OPEN) {
                log.warn("⚠️ Circuit Breaker '{}' is OPEN! Calls will be rejected.", name);
            }
        }
    }

    @Scheduled(fixedRate = 60000) // каждую минуту
    public void resetMetrics() {
        // Сброс метрик для предотвращения утечки памяти
        // Метод reset() не существует в CircuitBreaker.Metrics, поэтому просто логируем
        log.debug("Circuit breaker metrics reset scheduled - skipping (reset() method not available)");
    }
}