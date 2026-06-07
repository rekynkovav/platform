package com.platform.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Minimal Spring configuration for @DataJpaTest repository tests.
 * Scans ONLY entities and repositories — no security, cache, websocket,
 * health indicators, filters, or external service dependencies.
 */
@SpringBootApplication(
    scanBasePackages = "com.platform.repository",
    exclude = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration.class,
        org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration.class,
        org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration.class,
        org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration.class
    }
)
@EntityScan(basePackages = "com.platform.model")
@EnableJpaRepositories(basePackages = "com.platform.repository")
public class TestJpaConfig {
}
