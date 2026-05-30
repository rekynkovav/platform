package com.platform.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    // Circuit Breaker для Keycloak
    @Bean
    public CircuitBreaker keycloakCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50) // 50% failures trigger open state
            .slowCallRateThreshold(50) // 50% slow calls trigger open state
            .slowCallDurationThreshold(Duration.ofSeconds(2)) // Calls slower than 2s are slow
            .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s in open state
            .permittedNumberOfCallsInHalfOpenState(5) // Allow 5 calls in half-open state
            .minimumNumberOfCalls(10) // Need at least 10 calls to calculate failure rate
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(20) // Last 20 calls
            .recordExceptions(
                RuntimeException.class,
                Exception.class
            )
            .ignoreExceptions(
                IllegalArgumentException.class // Игнорируем ошибки валидации
            )
            .build();

        return CircuitBreakerRegistry.of(config).circuitBreaker("keycloak");
    }

    // Circuit Breaker для внешних API
    @Bean
    public CircuitBreaker externalApiCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(40)
            .slowCallRateThreshold(30)
            .slowCallDurationThreshold(Duration.ofSeconds(3))
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .permittedNumberOfCallsInHalfOpenState(3)
            .minimumNumberOfCalls(5)
            .slidingWindowSize(10)
            .build();

        return CircuitBreakerRegistry.of(config).circuitBreaker("external-api");
    }

    // Circuit Breaker для базы данных
    @Bean
    public CircuitBreaker databaseCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(30) // Более низкий порог для БД
            .slowCallRateThreshold(20)
            .slowCallDurationThreshold(Duration.ofSeconds(5)) // Более долгие таймауты для БД
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .permittedNumberOfCallsInHalfOpenState(2)
            .minimumNumberOfCalls(5)
            .slidingWindowSize(15)
            .recordExceptions(
                org.springframework.dao.DataAccessException.class,
                java.sql.SQLException.class
            )
            .build();

        return CircuitBreakerRegistry.of(config).circuitBreaker("database");
    }

    // Retry для Keycloak
    @Bean
    public Retry keycloakRetry() {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .retryExceptions(RuntimeException.class)
            .ignoreExceptions(IllegalArgumentException.class)
            .build();

        return RetryRegistry.of(config).retry("keycloak");
    }

    // Retry для внешних API
    @Bean
    public Retry externalApiRetry() {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(2)
            .waitDuration(Duration.ofSeconds(1))
            .retryExceptions(Exception.class)
            .build();

        return RetryRegistry.of(config).retry("external-api");
    }

    // Time Limiter для медленных операций
    @Bean
    public TimeLimiter slowOperationsTimeLimiter() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(10))
            .cancelRunningFuture(true)
            .build();

        return TimeLimiterRegistry.of(config).timeLimiter("slow-operations");
    }

    // Bulkhead для ограничения параллельных запросов
    @Bean
    public Bulkhead keycloakBulkhead() {
        BulkheadConfig config = BulkheadConfig.custom()
            .maxConcurrentCalls(10) // Максимум 10 параллельных запросов к Keycloak
            .maxWaitDuration(Duration.ofMillis(500)) // Максимальное время ожидания
            .build();

        return BulkheadRegistry.of(config).bulkhead("keycloak");
    }

    @Bean
    public Bulkhead databaseBulkhead() {
        BulkheadConfig config = BulkheadConfig.custom()
            .maxConcurrentCalls(20) // Максимум 20 параллельных запросов к БД
            .maxWaitDuration(Duration.ofMillis(1000))
            .build();

        return BulkheadRegistry.of(config).bulkhead("database");
    }

    // Конфигурация для мониторинга
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }

    @Bean
    public RetryRegistry retryRegistry() {
        return RetryRegistry.ofDefaults();
    }

    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        return TimeLimiterRegistry.ofDefaults();
    }

    @Bean
    public BulkheadRegistry bulkheadRegistry() {
        return BulkheadRegistry.ofDefaults();
    }
}