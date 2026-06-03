package com.platform.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UploadLimitServiceTest {

    private UploadLimitService uploadLimitService;
    private RedisTemplate<String, String> redisTemplate;
    private ValueOperations<String, String> valueOps;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        uploadLimitService = new UploadLimitService(redisTemplate);
    }

    // ==================== checkDailyLimit ====================

    @Test
    void checkDailyLimit_NoKey_ReturnsTrue() {
        when(valueOps.get(anyString())).thenReturn(null);

        assertTrue(uploadLimitService.checkDailyLimit("user-1"));
    }

    @Test
    void checkDailyLimit_BelowLimit_ReturnsTrue() {
        when(valueOps.get(anyString())).thenReturn("10");

        assertTrue(uploadLimitService.checkDailyLimit("user-1"));
    }

    @Test
    void checkDailyLimit_AtLimit_ReturnsFalse() {
        when(valueOps.get(anyString())).thenReturn("50");

        assertFalse(uploadLimitService.checkDailyLimit("user-1"));
    }

    @Test
    void checkDailyLimit_AboveLimit_ReturnsFalse() {
        when(valueOps.get(anyString())).thenReturn("51");

        assertFalse(uploadLimitService.checkDailyLimit("user-1"));
    }

    @Test
    void checkDailyLimit_UsesCorrectKeyFormat() {
        String userId = "test-user";
        String expectedKey = "upload_count:" + userId + ":" + LocalDate.now();
        when(valueOps.get(expectedKey)).thenReturn("0");

        uploadLimitService.checkDailyLimit(userId);

        verify(valueOps).get(expectedKey);
    }

    // ==================== incrementDailyCount ====================

    @Test
    void incrementDailyCount_IncrementsAndSetsExpiry() {
        String userId = "user-1";
        String expectedKey = "upload_count:" + userId + ":" + LocalDate.now();

        uploadLimitService.incrementDailyCount(userId);

        verify(valueOps).increment(expectedKey);
        verify(redisTemplate).expire(eq(expectedKey), eq(Duration.ofDays(1)));
    }

    // ==================== checkStorageLimit ====================

    @Test
    void checkStorageLimit_UnderLimit_ReturnsTrue() {
        long fileSize = 1024 * 1024; // 1MB
        long currentUsage = 1024 * 1024 * 100; // 100MB

        assertTrue(uploadLimitService.checkStorageLimit("user-1", fileSize, currentUsage));
    }

    @Test
    void checkStorageLimit_ExactlyAtLimit_ReturnsTrue() {
        long limit = 5L * 1024 * 1024 * 1024; // 5GB
        long currentUsage = limit - 1000;
        long fileSize = 1000;

        assertTrue(uploadLimitService.checkStorageLimit("user-1", fileSize, currentUsage));
    }

    @Test
    void checkStorageLimit_ExceedsLimit_ReturnsFalse() {
        long limit = 5L * 1024 * 1024 * 1024; // 5GB
        long currentUsage = limit - 100;
        long fileSize = 200;

        assertFalse(uploadLimitService.checkStorageLimit("user-1", fileSize, currentUsage));
    }

    @Test
    void checkStorageLimit_ZeroFileSize_ReturnsTrue() {
        long currentUsage = 5L * 1024 * 1024 * 1024; // exactly at limit

        assertTrue(uploadLimitService.checkStorageLimit("user-1", 0, currentUsage));
    }
}
