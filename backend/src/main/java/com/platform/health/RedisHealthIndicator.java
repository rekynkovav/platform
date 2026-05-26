package com.platform.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisHealthIndicator(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        try {
            String pong = redisTemplate.getConnectionFactory()
                .getConnection().ping();
            if ("PONG".equals(pong)) {
                return Health.up()
                    .withDetail("redis", "available")
                    .withDetail("host", redisTemplate.getConnectionFactory().getConnection().getClientName())
                    .build();
            }
        } catch (Exception e) {
            return Health.down(e)
                .withDetail("redis", "unavailable")
                .withDetail("error", e.getMessage())
                .build();
        }
        return Health.down()
            .withDetail("redis", "unknown")
            .build();
    }
}