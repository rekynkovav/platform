package com.platform.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class IpRateLimitFilter implements Filter {

    private final Cache<String, AtomicInteger> ipLimits = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .maximumSize(10000)
        .build();

    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final int MAX_REQUESTS_PER_SECOND = 20;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String ip = getClientIp(httpRequest);
        String path = httpRequest.getRequestURI();

        // Пропускаем health checks и статические ресурсы
        if (path.startsWith("/actuator/health") ||
            path.startsWith("/swagger-ui") ||
            path.startsWith("/api-docs") ||
            path.endsWith(".css") ||
            path.endsWith(".js") ||
            path.endsWith(".png") ||
            path.endsWith(".jpg")) {
            chain.doFilter(request, response);
            return;
        }

        // Разные лимиты для разных эндпоинтов
        int limit = getLimitForPath(path);

        String key = ip + ":" + path;
        AtomicInteger count = ipLimits.get(key, k -> new AtomicInteger(0));

        int currentCount = count.incrementAndGet();

        if (currentCount > limit) {
            log.warn("Rate limit exceeded for IP: {} on path: {} ({} requests)", ip, path, currentCount);
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(String.format(
                "{\"error\":\"Too many requests\",\"limit\":%d,\"retryAfter\":60}",
                limit
            ));
            return;
        }

        // Добавляем заголовки с информацией о лимитах
        httpResponse.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        httpResponse.setHeader("X-RateLimit-Remaining", String.valueOf(limit - currentCount));
        httpResponse.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + 60000));

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }

    private int getLimitForPath(String path) {
        if (path.startsWith("/api/auth/")) {
            return 10; // Более строгий лимит для аутентификации
        } else if (path.startsWith("/api/payments/")) {
            return 30; // Лимит для платежей
        } else if (path.startsWith("/api/chat/")) {
            return 50; // Лимит для чата
        } else if (path.startsWith("/api/")) {
            return MAX_REQUESTS_PER_MINUTE; // Стандартный лимит для API
        } else {
            return 200; // Более высокий лимит для статики
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("IP Rate Limit Filter initialized");
    }

    @Override
    public void destroy() {
        log.info("IP Rate Limit Filter destroyed");
    }
}