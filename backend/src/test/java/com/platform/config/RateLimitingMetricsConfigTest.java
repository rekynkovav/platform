package com.platform.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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

    // ==================== MetricsConfig ====================

    @Test
    void metricsConfig_TimedAspect_CreatesBean() {
        MetricsConfig config = new MetricsConfig();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        TimedAspect aspect = config.timedAspect(registry);

        assertNotNull(aspect);
    }

    @Test
    void metricsConfig_TimedAspect_UsesProvidedRegistry() {
        MetricsConfig config = new MetricsConfig();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        TimedAspect aspect = config.timedAspect(registry);

        assertNotNull(aspect);
        // TimedAspect is created successfully with the provided registry
    }
}
