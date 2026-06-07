package com.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorsCacheConfigTest {

    // ==================== CorsConfig ====================

    @Test
    void corsConfig_AddCorsMappings_DoesNotThrow() {
        CorsConfig config = new CorsConfig();
        CorsRegistry registry = new CorsRegistry();

        assertDoesNotThrow(() -> config.addCorsMappings(registry));
    }

    @Test
    void corsConfig_ImplementsWebMvcConfigurer() {
        CorsConfig config = new CorsConfig();
        assertInstanceOf(org.springframework.web.servlet.config.annotation.WebMvcConfigurer.class, config);
    }

    // ==================== CacheConfig ====================

    @Test
    void cacheConfig_CacheManager_ReturnsNonNull() {
        CacheConfig config = new CacheConfig();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        CacheManager cacheManager = config.cacheManager(connectionFactory);

        assertNotNull(cacheManager);
    }

    @Test
    void cacheConfig_CacheManager_HasUsersCache() {
        CacheConfig config = new CacheConfig();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        CacheManager cacheManager = config.cacheManager(connectionFactory);

        assertNotNull(cacheManager.getCache("users"));
    }

    @Test
    void cacheConfig_CacheManager_HasBroadcastsCache() {
        CacheConfig config = new CacheConfig();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        CacheManager cacheManager = config.cacheManager(connectionFactory);

        assertNotNull(cacheManager.getCache("broadcasts"));
    }

    @Test
    void cacheConfig_CacheManager_HasEventsCache() {
        CacheConfig config = new CacheConfig();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        CacheManager cacheManager = config.cacheManager(connectionFactory);

        assertNotNull(cacheManager.getCache("events"));
    }

    @Test
    void cacheConfig_CacheManager_HasMessagesCache() {
        CacheConfig config = new CacheConfig();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        CacheManager cacheManager = config.cacheManager(connectionFactory);

        assertNotNull(cacheManager.getCache("messages"));
    }
}
