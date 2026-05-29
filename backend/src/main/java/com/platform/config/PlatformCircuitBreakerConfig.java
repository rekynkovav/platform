package com.platform.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class PlatformCircuitBreakerConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }

    @Bean
    public CircuitBreaker databaseCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // 50% ошибок
                .slowCallRateThreshold(50) // 50% медленных вызовов
                .slowCallDurationThreshold(Duration.ofSeconds(2)) // 2 секунды - медленный вызов
                .waitDurationInOpenState(Duration.ofSeconds(30)) // 30 секунд в открытом состоянии
                .permittedNumberOfCallsInHalfOpenState(5) // 5 вызовов в полуоткрытом состоянии
                .minimumNumberOfCalls(10) // минимум 10 вызовов для расчета метрик
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(20) // окно из 20 вызовов
                .build();

        return registry.circuitBreaker("database", config);
    }

    @Bean
    public CircuitBreaker externalApiCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(30) // 30% ошибок
                .slowCallRateThreshold(30) // 30% медленных вызовов
                .slowCallDurationThreshold(Duration.ofSeconds(5)) // 5 секунд для внешних API
                .waitDurationInOpenState(Duration.ofSeconds(60)) // 60 секунд в открытом состоянии
                .permittedNumberOfCallsInHalfOpenState(3) // 3 вызова в полуоткрытом состоянии
                .minimumNumberOfCalls(5) // минимум 5 вызовов
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10) // окно из 10 вызовов
                .build();

        return registry.circuitBreaker("externalApi", config);
    }

    @Bean
    public Resilience4JCircuitBreakerFactory circuitBreakerFactory() {
        Resilience4JCircuitBreakerFactory factory = new Resilience4JCircuitBreakerFactory();
        factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(10))
                        .build())
                .build());
        return factory;
    }
}