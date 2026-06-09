package com.platform.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisWebSocketMessageSubscriber implements MessageListener {

    private final ChatWebSocketHandler chatWebSocketHandler;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String body = new String(message.getBody());

        log.debug("Received Redis pub/sub message from channel '{}': {}", channel, body);

        if (channel.startsWith("ws:message:")) {
            String userId = channel.substring("ws:message:".length());
            chatWebSocketHandler.sendNotificationToUser(userId, body);
        } else if (channel.startsWith("ws:room:")) {
            log.debug("Room message on {}: {}", channel, body);
        } else if ("ws:register".equals(channel) || "ws:unregister".equals(channel)) {
            log.info("Session event on {}: {}", channel, body);
        }
    }
}
