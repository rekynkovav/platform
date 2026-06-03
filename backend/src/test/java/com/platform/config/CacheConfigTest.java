package com.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CacheConfigTest {

    @Test
    void cacheManager_ReturnsRedisCacheManager() {
        CacheConfig config = new CacheConfig();
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);

        CacheManager manager = config.cacheManager(factory);

        assertNotNull(manager);
        assertInstanceOf(RedisCacheManager.class, manager);
    }

    @Test
    void cacheManager_HasConfiguredCaches() {
        CacheConfig config = new CacheConfig();
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);

        CacheManager manager = config.cacheManager(factory);

        assertNotNull(manager.getCache("users"));
        assertNotNull(manager.getCache("broadcasts"));
        assertNotNull(manager.getCache("events"));
        assertNotNull(manager.getCache("messages"));
    }

    @Test
    void cacheManager_DefaultCacheExists() {
        CacheConfig config = new CacheConfig();
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);

        CacheManager manager = config.cacheManager(factory);

        // Default cache should be available for any name not explicitly configured
        assertNotNull(manager.getCache("default"));
    }
}
