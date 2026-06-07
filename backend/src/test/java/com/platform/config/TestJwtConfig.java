package com.platform.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.mockito.Mockito.mock;

/**
 * Тестовая конфигурация для JWT.
 * Предоставляет mock JwtDecoder, который не пытается валидировать токены.
 * Используется в @WebMvcTest тестах контроллеров вместе с TestSecurityConfig.
 *
 * Без этого бина @WebMvcTest падает при старте, потому что
 * OAuth2ResourceServerAutoConfiguration требует JwtDecoder.
 */
@TestConfiguration
public class TestJwtConfig {

    @Bean
    public JwtDecoder jwtDecoder() {
        return mock(JwtDecoder.class);
    }
}
