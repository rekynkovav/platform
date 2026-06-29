package com.platform.integration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Test-only CacheConfig that overrides CacheConfig's RedisCacheManager.
 * Provides an in-memory ConcurrentMapCacheManager for integration tests.
 */
@Configuration
public class IntegrationTestCacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            "users", "posts", "events", "broadcasts", "messages",
            "unreadNotifications", "allNotifications", "chatMessages",
            "chatMessagesPaginated", "questionsByMode", "userRoles"
        );
    }
}
