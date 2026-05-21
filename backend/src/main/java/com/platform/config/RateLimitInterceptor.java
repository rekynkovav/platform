package com.platform.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // Лимиты: ключ -> (лимит, время в секундах)
    private static final Map<String, RateLimit> LIMITS = Map.of(
        "broadcast_create", new RateLimit(5, 60),   // 5 созданий трансляций в минуту
        "broadcast_start", new RateLimit(10, 60),   // 10 запусков в минуту
        "chat_message", new RateLimit(30, 60),      // 30 сообщений в минуту
        "upload_file", new RateLimit(5, 300),       // 5 загрузок в 5 минут
        "default", new RateLimit(100, 60)           // 100 запросов в минуту по умолчанию
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = getUserId(request);
        if (userId == null) {
            return true; // Неавторизованные запросы не лимитируем
        }

        String action = getActionFromPath(request.getRequestURI());
        RateLimit limit = LIMITS.getOrDefault(action, LIMITS.get("default"));

        String key = userId + ":" + action;
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(limit.limit, limit.durationSeconds));

        if (bucket.tryConsume(1)) {
            return true;
        } else {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(limit.durationSeconds));

            log.warn("Rate limit exceeded for user {} on action {}", userId, action);
            log.warn("Превышен лимит запросов для пользователя {} на действие {}", userId, action);

            return false;
        }
    }

    private Bucket createBucket(int limit, int durationSeconds) {
        return Bucket4j.builder()
            .addLimit(Bandwidth.classic(limit,
                Refill.greedy(limit, Duration.ofSeconds(durationSeconds))))
            .build();
    }

    private String getUserId(HttpServletRequest request) {
        // Получаем userId из SecurityContext
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    private String getActionFromPath(String path) {
        if (path.contains("/broadcast/create")) return "broadcast_create";
        if (path.contains("/broadcast/") && path.contains("/start")) return "broadcast_start";
        if (path.contains("/chat/message")) return "chat_message";
        if (path.contains("/media/upload")) return "upload_file";
        return "default";
    }

    private static class RateLimit {
        final int limit;
        final int durationSeconds;

        RateLimit(int limit, int durationSeconds) {
            this.limit = limit;
            this.durationSeconds = durationSeconds;
        }
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RateLimitInterceptor.class);
}