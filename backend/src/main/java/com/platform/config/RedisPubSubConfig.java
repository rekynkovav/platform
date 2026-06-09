package com.platform.config;

import com.platform.websocket.RedisWebSocketMessageSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import java.util.concurrent.Executors;

@Configuration
public class RedisPubSubConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisWebSocketMessageSubscriber subscriber) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setTaskExecutor(Executors.newFixedThreadPool(10));

        container.addMessageListener(subscriber, new PatternTopic("ws:register"));
        container.addMessageListener(subscriber, new PatternTopic("ws:unregister"));
        container.addMessageListener(subscriber, new PatternTopic("ws:message:*"));
        container.addMessageListener(subscriber, new PatternTopic("ws:room:*"));

        return container;
    }
}
