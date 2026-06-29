package com.platform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

/**
 * Broadcast reactions service — ephemeral reaction counts stored in Redis.
 * Reactions expire after 6 hours.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BroadcastReactionService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String REACTION_PREFIX = "broadcast:reactions:";
    private static final String REACTION_LOG_PREFIX = "broadcast:reactions:log:";
    private static final Duration REACTION_TTL = Duration.ofHours(6);
    private static final int MAX_REACTIONS_PER_USER = 30; // per minute
    private static final String RATE_LIMIT_PREFIX = "broadcast:ratelimit:";

    public record ReactionSummary(String broadcastId, Map<String, Long> counts, long total) {}

    /**
     * Add a reaction to a broadcast.
     * Returns the updated summary.
     */
    public ReactionSummary addReaction(String broadcastId, String userId, String emoji) {
        String countKey = REACTION_PREFIX + broadcastId;
        String rateLimitKey = RATE_LIMIT_PREFIX + broadcastId + ":" + userId;

        // Rate limit: max 30 reactions per user per minute
        Long currentCount = redisTemplate.opsForValue().increment(rateLimitKey);
        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(rateLimitKey, Duration.ofMinutes(1));
        }
        if (currentCount != null && currentCount > MAX_REACTIONS_PER_USER) {
            log.warn("Reaction rate limit exceeded: broadcast={} user={}", broadcastId, userId);
            return getSummary(broadcastId);
        }

        // Increment emoji count
        redisTemplate.opsForHash().increment(countKey, emoji, 1);
        redisTemplate.expire(countKey, REACTION_TTL);

        // Log individual reaction
        String logKey = REACTION_LOG_PREFIX + broadcastId;
        redisTemplate.opsForList().rightPush(logKey, userId + ":" + emoji + ":" + System.currentTimeMillis());
        redisTemplate.expire(logKey, REACTION_TTL);

        // Trim log to last 1000
        Long logSize = redisTemplate.opsForList().size(logKey);
        if (logSize != null && logSize > 1000) {
            redisTemplate.opsForList().trim(logKey, logSize - 1000, -1);
        }

        log.debug("Reaction added: broadcast={} emoji={} user={}", broadcastId, emoji, userId);
        return getSummary(broadcastId);
    }

    /**
     * Get reaction summary for a broadcast.
     */
    public ReactionSummary getSummary(String broadcastId) {
        String countKey = REACTION_PREFIX + broadcastId;
        Map<Object, Object> rawCounts = redisTemplate.opsForHash().entries(countKey);

        Map<String, Long> counts = new LinkedHashMap<>();
        long total = 0;
        for (Map.Entry<Object, Object> entry : rawCounts.entrySet()) {
            long count = Long.parseLong(entry.getValue().toString());
            counts.put(entry.getKey().toString(), count);
            total += count;
        }

        return new ReactionSummary(broadcastId, counts, total);
    }

    /**
     * Clear all reactions for a broadcast.
     */
    public void clearReactions(String broadcastId) {
        redisTemplate.delete(REACTION_PREFIX + broadcastId);
        redisTemplate.delete(REACTION_LOG_PREFIX + broadcastId);
        log.info("Cleared reactions for broadcast {}", broadcastId);
    }
}
