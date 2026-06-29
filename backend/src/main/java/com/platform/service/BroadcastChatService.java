package com.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.util.XssSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Broadcast chat service — ephemeral messages stored in Redis.
 * Messages expire after 6 hours (live chat doesn't need persistence).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BroadcastChatService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final XssSanitizer xssSanitizer;

    private static final String CHAT_PREFIX = "broadcast:chat:";
    private static final String CHAT_INDEX_PREFIX = "broadcast:chat:idx:";
    private static final Duration MESSAGE_TTL = Duration.ofHours(6);
    private static final int MAX_MESSAGES_PER_BROADCAST = 500;

    public record ChatMessage(
        String id,
        String broadcastId,
        String userId,
        String username,
        String userRole,
        String content,
        String type,
        String timestamp
    ) {}

    /**
     * Send a chat message to a broadcast.
     */
    public ChatMessage sendMessage(String broadcastId, String userId, String username,
                                    String userRole, String content, String type) {
        String messageId = "bchat-" + UUID.randomUUID().toString().substring(0, 12);
        String timestamp = java.time.Instant.now().toString();

        // Sanitize user-provided text to prevent XSS in chat display
        String sanitizedContent = xssSanitizer.sanitize(content);
        String sanitizedUsername = xssSanitizer.sanitize(username);

        ChatMessage message = new ChatMessage(
            messageId, broadcastId, userId, sanitizedUsername, userRole, sanitizedContent,
            type != null ? type : "message", timestamp
        );

        try {
            String json = objectMapper.writeValueAsString(message);
            String listKey = CHAT_PREFIX + broadcastId;
            String indexKey = CHAT_INDEX_PREFIX + broadcastId;

            // Add to list (trimmed to max size)
            redisTemplate.opsForList().rightPush(listKey, json);
            redisTemplate.expire(listKey, MESSAGE_TTL);

            // Trim to keep only last N messages
            Long size = redisTemplate.opsForList().size(listKey);
            if (size != null && size > MAX_MESSAGES_PER_BROADCAST) {
                redisTemplate.opsForList().trim(listKey, size - MAX_MESSAGES_PER_BROADCAST, -1);
            }

            // Track message ID
            redisTemplate.opsForSet().add(indexKey, messageId);
            redisTemplate.expire(indexKey, MESSAGE_TTL);

            log.debug("Broadcast chat message: broadcast={} user={}", broadcastId, username);
            return message;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize chat message", e);
            throw new RuntimeException("Failed to send chat message", e);
        }
    }

    /**
     * Get recent chat messages for a broadcast.
     */
    public List<ChatMessage> getMessages(String broadcastId, int limit) {
        String listKey = CHAT_PREFIX + broadcastId;

        long size = redisTemplate.opsForList().size(listKey);
        if (size == 0) return Collections.emptyList();

        long start = Math.max(0, size - limit);
        List<String> jsonMessages = redisTemplate.opsForList().range(listKey, start, -1);
        if (jsonMessages == null) return Collections.emptyList();

        return jsonMessages.stream()
            .map(json -> {
                try {
                    return objectMapper.readValue(json, ChatMessage.class);
                } catch (JsonProcessingException e) {
                    log.warn("Failed to deserialize chat message", e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Clear all chat messages for a broadcast (e.g., on broadcast end).
     */
    public void clearMessages(String broadcastId) {
        redisTemplate.delete(CHAT_PREFIX + broadcastId);
        redisTemplate.delete(CHAT_INDEX_PREFIX + broadcastId);
        log.info("Cleared chat messages for broadcast {}", broadcastId);
    }
}
