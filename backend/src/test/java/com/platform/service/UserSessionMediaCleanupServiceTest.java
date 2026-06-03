package com.platform.service;

import com.platform.model.MediaFile;
import com.platform.repository.MediaFileRepository;
import com.platform.websocket.ChatWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserSessionMediaCleanupServiceTest {

    // ==================== UserSessionService ====================

    @Test
    void userSession_InvalidateAllSessions_ClosesWebSocketAndBlacklistsToken() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        ChatWebSocketHandler wsHandler = mock(ChatWebSocketHandler.class);

        UserSessionService service = new UserSessionService(redis, wsHandler);
        service.invalidateAllSessions("user-1");

        verify(wsHandler).closeUserSessions("user-1");
        verify(ops).set(eq("blacklisted_tokens:user-1"), anyString());
        verify(redis).expire(eq("blacklisted_tokens:user-1"), any());
    }

    @Test
    void userSession_IsTokenBlacklisted_TrueWhenKeyExists() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
        when(redis.hasKey("blacklisted_tokens:user-1")).thenReturn(true);
        ChatWebSocketHandler wsHandler = mock(ChatWebSocketHandler.class);

        UserSessionService service = new UserSessionService(redis, wsHandler);
        assertTrue(service.isTokenBlacklisted("user-1"));
    }

    @Test
    void userSession_IsTokenBlacklisted_FalseWhenKeyMissing() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
        when(redis.hasKey("blacklisted_tokens:user-2")).thenReturn(false);
        ChatWebSocketHandler wsHandler = mock(ChatWebSocketHandler.class);

        UserSessionService service = new UserSessionService(redis, wsHandler);
        assertFalse(service.isTokenBlacklisted("user-2"));
    }

    @Test
    void userSession_IsTokenBlacklisted_NullResult_ReturnsFalse() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
        when(redis.hasKey(anyString())).thenReturn(null);
        ChatWebSocketHandler wsHandler = mock(ChatWebSocketHandler.class);

        UserSessionService service = new UserSessionService(redis, wsHandler);
        assertFalse(service.isTokenBlacklisted("user-3"));
    }

    // ==================== MediaCleanupService ====================

    @Test
    void mediaCleanup_CleanupOldHlsSegments_DeletesOldSegments() {
        MediaFileRepository repo = mock(MediaFileRepository.class);
        StorageService storage = mock(StorageService.class);

        MediaFile segment = new MediaFile();
        segment.setId(UUID.randomUUID().toString());
        segment.setS3Key("hls/old-segment.ts");
        when(repo.findOldHlsSegments(any(LocalDateTime.class))).thenReturn(List.of(segment));

        MediaCleanupService service = new MediaCleanupService(repo, storage);
        service.cleanupOldHlsSegments();

        verify(storage).delete("hls/old-segment.ts");
        verify(repo).delete(segment);
    }

    @Test
    void mediaCleanup_CleanupOldHlsSegments_NoSegments_NoDeletion() {
        MediaFileRepository repo = mock(MediaFileRepository.class);
        StorageService storage = mock(StorageService.class);
        when(repo.findOldHlsSegments(any(LocalDateTime.class))).thenReturn(List.of());

        MediaCleanupService service = new MediaCleanupService(repo, storage);
        service.cleanupOldHlsSegments();

        verifyNoInteractions(storage);
        verify(repo, never()).delete(any());
    }

    @Test
    void mediaCleanup_CleanupOldHlsSegments_DeleteFails_ContinuesProcessing() {
        MediaFileRepository repo = mock(MediaFileRepository.class);
        StorageService storage = mock(StorageService.class);

        MediaFile seg1 = new MediaFile();
        seg1.setId(UUID.randomUUID().toString());
        seg1.setS3Key("seg1.ts");
        MediaFile seg2 = new MediaFile();
        seg2.setId(UUID.randomUUID().toString());
        seg2.setS3Key("seg2.ts");

        when(repo.findOldHlsSegments(any(LocalDateTime.class))).thenReturn(List.of(seg1, seg2));
        doThrow(new RuntimeException("S3 error")).when(storage).delete("seg1.ts");

        MediaCleanupService service = new MediaCleanupService(repo, storage);
        assertDoesNotThrow(service::cleanupOldHlsSegments);

        // Second segment should still be processed
        verify(storage).delete("seg2.ts");
        verify(repo).delete(seg2);
    }
}
