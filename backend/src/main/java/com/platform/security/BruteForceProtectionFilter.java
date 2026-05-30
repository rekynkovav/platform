package com.platform.security;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;

@Component
public class BruteForceProtectionFilter implements Filter {

    private final RedisTemplate<String, String> redisTemplate;
    private static final int MAX_ATTEMPTS = 5;
    private static final int BLOCK_DURATION_MINUTES = 15;

    public BruteForceProtectionFilter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if ("/login".equals(httpRequest.getRequestURI()) && "POST".equals(httpRequest.getMethod())) {
            String ip = httpRequest.getRemoteAddr();
            String key = "login_fail:" + ip;

            String attempts = redisTemplate.opsForValue().get(key);
            if (attempts != null && Integer.parseInt(attempts) >= MAX_ATTEMPTS) {
                httpResponse.sendError(429, "Too many login attempts. Try again in " + BLOCK_DURATION_MINUTES + " minutes");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    public void loginFailed(String ip) {
        String key = "login_fail:" + ip;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofMinutes(BLOCK_DURATION_MINUTES));
    }

    public void loginSuccess(String ip) {
        redisTemplate.delete("login_fail:" + ip);
    }
}