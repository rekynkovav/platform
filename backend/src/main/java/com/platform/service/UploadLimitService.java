package com.platform.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDate;

@Service
public class UploadLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final int DAILY_LIMIT = 50;
    private static final long STORAGE_LIMIT_BYTES = 5L * 1024 * 1024 * 1024; // 5GB

    public UploadLimitService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean checkDailyLimit(String userId) {
        String key = "upload_count:" + userId + ":" + LocalDate.now();
        String count = redisTemplate.opsForValue().get(key);
        int current = count == null ? 0 : Integer.parseInt(count);
        return current < DAILY_LIMIT;
    }

    public void incrementDailyCount(String userId) {
        String key = "upload_count:" + userId + ":" + LocalDate.now();
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofDays(1));
    }

    public boolean checkStorageLimit(String userId, long fileSize, long currentUsage) {
        return currentUsage + fileSize <= STORAGE_LIMIT_BYTES;
    }

    public boolean checkStorageLimit(String userId, long fileSize) {
        String key = "upload_storage:" + userId;
        String usageStr = redisTemplate.opsForValue().get(key);
        long currentUsage = usageStr == null ? 0 : Long.parseLong(usageStr);
        return currentUsage + fileSize <= STORAGE_LIMIT_BYTES;
    }
}