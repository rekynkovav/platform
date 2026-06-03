package com.platform.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UploadLimitMetricsServiceTest {

    // ==================== UploadLimitService ====================

    @Test
    void uploadLimit_CheckDailyLimit_BelowLimit_ReturnsTrue() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn("10");

        UploadLimitService service = new UploadLimitService(redis);
        assertTrue(service.checkDailyLimit("user-1"));
    }

    @Test
    void uploadLimit_CheckDailyLimit_AtLimit_ReturnsFalse() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn("50");

        UploadLimitService service = new UploadLimitService(redis);
        assertFalse(service.checkDailyLimit("user-1"));
    }

    @Test
    void uploadLimit_CheckDailyLimit_NullCount_ReturnsTrue() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn(null);

        UploadLimitService service = new UploadLimitService(redis);
        assertTrue(service.checkDailyLimit("user-1"));
    }

    @Test
    void uploadLimit_IncrementDailyCount_CallsRedisIncrementAndExpire() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);

        UploadLimitService service = new UploadLimitService(redis);
        service.incrementDailyCount("user-1");

        verify(ops).increment(anyString());
        verify(redis).expire(anyString(), any());
    }

    @Test
    void uploadLimit_CheckStorageLimit_WithinLimit_ReturnsTrue() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
        UploadLimitService service = new UploadLimitService(redis);

        long fileSize = 1024 * 1024; // 1MB
        long currentUsage = 1024L * 1024 * 1024; // 1GB
        assertTrue(service.checkStorageLimit("user-1", fileSize, currentUsage));
    }

    @Test
    void uploadLimit_CheckStorageLimit_ExceedsLimit_ReturnsFalse() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
        UploadLimitService service = new UploadLimitService(redis);

        long fileSize = 1024L * 1024 * 1024; // 1GB
        long currentUsage = 5L * 1024 * 1024 * 1024; // 5GB (at limit)
        assertFalse(service.checkStorageLimit("user-1", fileSize, currentUsage));
    }

    // ==================== MetricsService ====================

    @Test
    void metricsService_IncrementMessages_DoesNotThrow() {
        MeterRegistry registry = new SimpleMeterRegistry();
        MetricsService service = new MetricsService(registry);

        assertDoesNotThrow(service::incrementMessages);
        assertEquals(1.0, registry.find("chat.messages.total").counter().count());
    }

    @Test
    void metricsService_IncrementBroadcasts_DoesNotThrow() {
        MeterRegistry registry = new SimpleMeterRegistry();
        MetricsService service = new MetricsService(registry);

        assertDoesNotThrow(service::incrementBroadcasts);
        assertEquals(1.0, registry.find("broadcast.total").counter().count());
    }

    @Test
    void metricsService_IncrementErrors_DoesNotThrow() {
        MeterRegistry registry = new SimpleMeterRegistry();
        MetricsService service = new MetricsService(registry);

        assertDoesNotThrow(service::incrementErrors);
        assertEquals(1.0, registry.find("errors.total").counter().count());
    }

    @Test
    void metricsService_LoginCounters_Work() {
        MeterRegistry registry = new SimpleMeterRegistry();
        MetricsService service = new MetricsService(registry);

        service.incrementLoginSuccess();
        service.incrementLoginSuccess();
        service.incrementLoginFailure();

        assertEquals(2.0, registry.find("auth.login.success").counter().count());
        assertEquals(1.0, registry.find("auth.login.failure").counter().count());
    }

    @Test
    void metricsService_Timer_StartAndStop_DoesNotThrow() {
        MeterRegistry registry = new SimpleMeterRegistry();
        MetricsService service = new MetricsService(registry);

        var sample = service.startTimer();
        assertDoesNotThrow(() -> service.stopTimer(sample));
    }

    @Test
    void metricsService_StopTimer_NullSample_DoesNotThrow() {
        MeterRegistry registry = new SimpleMeterRegistry();
        MetricsService service = new MetricsService(registry);

        assertDoesNotThrow(() -> service.stopTimer(null));
    }

    @Test
    void metricsService_UpdateMessageRate_SetsGauge() {
        MeterRegistry registry = new SimpleMeterRegistry();
        MetricsService service = new MetricsService(registry);

        service.incrementMessages();
        service.incrementMessages();
        service.updateMessageRate();

        assertNotNull(registry.find("chat.messages.rate").gauge());
        assertEquals(2.0, registry.find("chat.messages.rate").gauge().value());
    }
}
