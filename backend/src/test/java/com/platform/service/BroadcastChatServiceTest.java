package com.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.util.XssSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BroadcastChatServiceTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ObjectMapper objectMapper;
    @Mock private XssSanitizer xssSanitizer;

    @InjectMocks
    private BroadcastChatService broadcastChatService;

    private static final String BROADCAST_ID = "bcast-001";

    // ==================== sendMessage ====================

    @Test
    void sendMessage_SanitizesAndReturnsMessage() throws Exception {
        ListOperations<String, String> listOps = mock(ListOperations.class);
        SetOperations<String, String> setOps = mock(SetOperations.class);
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);

        when(xssSanitizer.sanitize("Hello <script>")).thenReturn("Hello &lt;script&gt;");
        when(xssSanitizer.sanitize("User1")).thenReturn("User1");
        when(objectMapper.writeValueAsString(any(BroadcastChatService.ChatMessage.class)))
                .thenReturn("{\"id\":\"bchat-123\"}");
        when(listOps.rightPush(anyString(), anyString())).thenReturn(1L);
        when(listOps.size(anyString())).thenReturn(1L);
        when(setOps.add(anyString(), anyString())).thenReturn(1L);

        BroadcastChatService.ChatMessage result = broadcastChatService.sendMessage(
                BROADCAST_ID, "user-1", "User1", "USER", "Hello <script>", null);

        assertThat(result).isNotNull();
        assertThat(result.broadcastId()).isEqualTo(BROADCAST_ID);
        assertThat(result.userId()).isEqualTo("user-1");
        assertThat(result.content()).isEqualTo("Hello &lt;script&gt;");
        assertThat(result.username()).isEqualTo("User1");
        assertThat(result.type()).isEqualTo("message"); // default type
        verify(redisTemplate, atLeastOnce()).expire(anyString(), any());
    }

    @Test
    void sendMessage_WithExplicitType_UsesProvidedType() throws Exception {
        ListOperations<String, String> listOps = mock(ListOperations.class);
        SetOperations<String, String> setOps = mock(SetOperations.class);
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);

        when(xssSanitizer.sanitize(anyString())).thenReturn("clean");
        when(objectMapper.writeValueAsString(any(BroadcastChatService.ChatMessage.class)))
                .thenReturn("{}");
        when(listOps.rightPush(anyString(), anyString())).thenReturn(1L);
        when(listOps.size(anyString())).thenReturn(1L);
        when(setOps.add(anyString(), anyString())).thenReturn(1L);

        BroadcastChatService.ChatMessage result = broadcastChatService.sendMessage(
                BROADCAST_ID, "user-1", "User1", "ADMIN", "text", "reaction");

        assertThat(result.type()).isEqualTo("reaction");
    }

    @Test
    void sendMessage_ListExceedsMax_Trimmed() throws Exception {
        ListOperations<String, String> listOps = mock(ListOperations.class);
        SetOperations<String, String> setOps = mock(SetOperations.class);
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);

        when(xssSanitizer.sanitize(anyString())).thenReturn("clean");
        when(objectMapper.writeValueAsString(any(BroadcastChatService.ChatMessage.class)))
                .thenReturn("{}");
        when(listOps.rightPush(anyString(), anyString())).thenReturn(1L);
        when(listOps.size(anyString())).thenReturn(501L); // exceeds MAX_MESSAGES_PER_BROADCAST
        when(setOps.add(anyString(), anyString())).thenReturn(1L);

        broadcastChatService.sendMessage(BROADCAST_ID, "user-1", "User1", "USER", "text", null);

        verify(listOps).trim(anyString(), eq(1L), eq(-1L));
    }

    // ==================== getMessages ====================

    @Test
    void getMessages_EmptyList_ReturnsEmpty() {
        ListOperations<String, String> listOps = mock(ListOperations.class);
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.size("broadcast:chat:" + BROADCAST_ID)).thenReturn(0L);

        List<BroadcastChatService.ChatMessage> result =
                broadcastChatService.getMessages(BROADCAST_ID, 50);

        assertThat(result).isEmpty();
    }

    @Test
    void getMessages_NullRange_ReturnsEmpty() {
        ListOperations<String, String> listOps = mock(ListOperations.class);
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.size("broadcast:chat:" + BROADCAST_ID)).thenReturn(5L);
        when(listOps.range(anyString(), anyLong(), anyLong())).thenReturn(null);

        List<BroadcastChatService.ChatMessage> result =
                broadcastChatService.getMessages(BROADCAST_ID, 50);

        assertThat(result).isEmpty();
    }

    @Test
    void getMessages_WithMessages_DeserializesAndReturns() throws Exception {
        ListOperations<String, String> listOps = mock(ListOperations.class);
        when(redisTemplate.opsForList()).thenReturn(listOps);

        BroadcastChatService.ChatMessage msg = new BroadcastChatService.ChatMessage(
                "bchat-001", BROADCAST_ID, "user-1", "User1", "USER", "Hello", "message", "2024-01-01T00:00:00Z");

        when(listOps.size("broadcast:chat:" + BROADCAST_ID)).thenReturn(3L);
        when(listOps.range(anyString(), eq(0L), eq(-1L)))
                .thenReturn(List.of("{\"id\":\"bchat-001\"}"));
        when(objectMapper.readValue(eq("{\"id\":\"bchat-001\"}"), eq(BroadcastChatService.ChatMessage.class)))
                .thenReturn(msg);

        List<BroadcastChatService.ChatMessage> result =
                broadcastChatService.getMessages(BROADCAST_ID, 50);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("bchat-001");
    }

    // ==================== clearMessages ====================

    @Test
    void clearMessages_DeletesBothKeys() {
        broadcastChatService.clearMessages(BROADCAST_ID);

        verify(redisTemplate).delete("broadcast:chat:" + BROADCAST_ID);
        verify(redisTemplate).delete("broadcast:chat:idx:" + BROADCAST_ID);
    }
}
