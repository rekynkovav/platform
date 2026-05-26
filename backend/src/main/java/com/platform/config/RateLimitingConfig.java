package com.platform.config;

import com.giffing.bucket4j.spring.boot.starter.config.Bucket4JAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@Import(Bucket4JAutoConfiguration.class)
public class RateLimitingConfig {

    @Bean
    public RedisTemplate<String, Object> bucket4jRedisTemplate(LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        return template;
    }
}
