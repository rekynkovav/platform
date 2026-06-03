package com.platform.config;

import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.lang.reflect.Method;

/**
 * Тестовая конфигурация для отключения Rate Limiting в @WebMvcTest.
 * Bucket4j требует специальных бинов для каждой named rate limit,
 * которые не загружаются в @WebMvcTest контексте.
 * 
 * Этот конфиг предоставляет no-op implementation, чтобы аннотации
 * @RateLimiting не вызывали исключений во время тестов.
 */
@TestConfiguration
public class TestRateLimitingConfig {

    /**
     * No-op interceptor для rate limiting.
     * В тестах мы не проверяем rate limiting, поэтому просто пропускаем все запросы.
     */
    @Bean
    @Primary
    public Object rateLimitingInterceptor() {
        // Возвращаем простой объект — Spring AOP проигнорирует его,
        // так как он не является настоящим interceptor'ом.
        // Главное — чтобы бин существовал и контекст стартовал.
        return new Object();
    }
}
