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

    // Circuit Breaker для Keycloak (unique - not in PlatformCircuitBreakerConfig)
    @Bean
    public CircuitBreaker keycloakCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .slowCallRateThreshold(50)
            .slowCallDurationThreshold(Duration.ofSeconds(2))
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(5)
            .minimumNumberOfCalls(10)
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(20)
            .recordExceptions(RuntimeException.class, Exception.class)
            .ignoreExceptions(IllegalArgumentException.class)
            .build();

        return CircuitBreakerRegistry.of(config).circuitBreaker("keycloak");
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
            .maxConcurrentCalls(10)
            .maxWaitDuration(Duration.ofMillis(500))
            .build();

        return BulkheadRegistry.of(config).bulkhead("keycloak");
    }

    @Bean
    public Bulkhead databaseBulkhead() {
        BulkheadConfig config = BulkheadConfig.custom()
            .maxConcurrentCalls(20)
            .maxWaitDuration(Duration.ofMillis(1000))
            .build();

        return BulkheadRegistry.of(config).bulkhead("database");
    }

    // Registries (duplicates removed - defined in PlatformCircuitBreakerConfig)
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
