package com.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RateLimitingMetricsConfigTest {

    // ==================== RateLimitingConfig ====================

    @Test
    void rateLimitingConfig_Bucket4jRedisTemplate_CreatesTemplate() {
        RateLimitingConfig config = new RateLimitingConfig();
        LettuceConnectionFactory factory = mock(LettuceConnectionFactory.class);

        RedisTemplate<String, Object> template = config.bucket4jRedisTemplate(factory);

        assertNotNull(template);
        assertEquals(factory, template.getConnectionFactory());
    }
}
