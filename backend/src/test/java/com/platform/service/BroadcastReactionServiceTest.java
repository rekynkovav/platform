package com.platform.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BroadcastReactionServiceTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private HashOperations<String, Object, Object> hashOperations;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private ListOperations<String, String> listOperations;

    @InjectMocks
    private BroadcastReactionService broadcastReactionService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
    }

    @Test
    void addReaction_incrementsEmojiCount() {
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(hashOperations.increment(anyString(), anyString(), anyLong())).thenReturn(1L);
        when(hashOperations.entries(anyString())).thenReturn(Map.of("🔥", 1L));
        when(listOperations.size(anyString())).thenReturn(1L);

        BroadcastReactionService.ReactionSummary summary =
            broadcastReactionService.addReaction("bcast-001", "user-1", "🔥");

        assertThat(summary.total()).isEqualTo(1);
        assertThat(summary.counts()).containsEntry("🔥", 1L);
        verify(redisTemplate, atLeastOnce()).expire(anyString(), any());
    }

    @Test
    void addReaction_rateLimitExceeded_returnsExistingSummary() {
        when(valueOperations.increment(anyString())).thenReturn(31L);
        when(hashOperations.entries(anyString())).thenReturn(Map.of("❤️", 5L));

        BroadcastReactionService.ReactionSummary summary =
            broadcastReactionService.addReaction("bcast-001", "user-1", "❤️");

        assertThat(summary.total()).isEqualTo(5);
        verify(hashOperations, never()).increment(anyString(), anyString(), anyLong());
    }

    @Test
    void getSummary_returnsSortedCounts() {
        Map<Object, Object> raw = new LinkedHashMap<>();
        raw.put("🔥", "10");
        raw.put("❤️", "5");
        raw.put("👏", "3");
        when(hashOperations.entries("broadcast:reactions:bcast-001")).thenReturn(raw);

        BroadcastReactionService.ReactionSummary summary =
            broadcastReactionService.getSummary("bcast-001");

        assertThat(summary.total()).isEqualTo(18);
        assertThat(summary.counts()).hasSize(3);
    }

    @Test
    void clearReactions_deletesKeys() {
        broadcastReactionService.clearReactions("bcast-001");

        verify(redisTemplate).delete("broadcast:reactions:bcast-001");
        verify(redisTemplate).delete("broadcast:reactions:log:bcast-001");
    }
}
