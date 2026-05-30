package com.platform.monitoring;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class HotKeyMonitor {

    private final Cache<String, AtomicLong> hitCounter = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build();

    public void recordHit(String key) {
        hitCounter.get(key, k -> new AtomicLong()).incrementAndGet();
    }

    @Scheduled(fixedRate = 60000) // Каждую минуту
    public void logHotKeys() {
        hitCounter.asMap().entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()))
            .limit(10)
            .forEach(entry -> log.info("Hot key: {} -> {} hits", entry.getKey(), entry.getValue().get()));
    }

    public void recordCacheHit(String cacheName, String key) {
        recordHit("cache:" + cacheName + ":" + key);
    }

    public void recordApiHit(String endpoint, String method) {
        recordHit("api:" + method + ":" + endpoint);
    }

    public void recordWebSocketEvent(String eventType, String roomId) {
        recordHit("ws:" + eventType + ":" + roomId);
    }

    public void recordDatabaseQuery(String table, String operation) {
        recordHit("db:" + table + ":" + operation);
    }
}