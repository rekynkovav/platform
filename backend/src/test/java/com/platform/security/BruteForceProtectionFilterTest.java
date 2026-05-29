package com.platform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BruteForceProtectionFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private BruteForceProtectionFilter bruteForceFilter;
    private ConcurrentHashMap<String, AtomicInteger> failedAttempts;
    private ConcurrentHashMap<String, Long> lockouts;

    @BeforeEach
    void setUp() {
        failedAttempts = new ConcurrentHashMap<>();
        lockouts = new ConcurrentHashMap<>();
        bruteForceFilter = new BruteForceProtectionFilter(failedAttempts, lockouts);
    }

    @Test
    void testDoFilter_BelowLimit_RequestPasses() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getMethod()).thenReturn("POST");

        // 4 неудачные попытки (лимит 5)
        failedAttempts.put("192.168.1.100", new AtomicInteger(4));

        bruteForceFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void testDoFilter_AboveLimit_Returns429() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getMethod()).thenReturn("POST");

        // 6 неудачных попыток (лимит 5)
        failedAttempts.put("192.168.1.100", new AtomicInteger(6));
        // IP заблокирован на 15 минут
        lockouts.put("192.168.1.100", System.currentTimeMillis() + 900_000L);

        bruteForceFilter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(429);
        verify(response).setHeader(eq("X-RateLimit-Retry-After"), anyString());
    }

    @Test
    void testDoFilter_NonLoginEndpoint_SkipsCheck() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users/profile");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getMethod()).thenReturn("GET");

        // Даже с большим количеством неудачных попыток
        failedAttempts.put("192.168.1.100", new AtomicInteger(10));

        bruteForceFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void testDoFilter_LoginFailed_IncrementsCounter() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getMethod()).thenReturn("POST");
        when(request.getAttribute("login.failed")).thenReturn(true);

        bruteForceFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        // Проверяем, что счетчик увеличился
        assertThat(failedAttempts.get("192.168.1.100").get()).isEqualTo(1);
    }

    @Test
    void testDoFilter_LoginSuccess_RemovesCounter() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getMethod()).thenReturn("POST");
        when(request.getAttribute("login.success")).thenReturn(true);

        // Предварительно есть неудачные попытки
        failedAttempts.put("192.168.1.100", new AtomicInteger(3));

        bruteForceFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        // Проверяем, что счетчик удален после успешного входа
        assertThat(failedAttempts).doesNotContainKey("192.168.1.100");
    }

    @Test
    void testDoFilter_LockoutExpired_ResetsCounter() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getMethod()).thenReturn("POST");

        // IP был заблокирован, но блокировка истекла (15 минут назад)
        failedAttempts.put("192.168.1.100", new AtomicInteger(6));
        lockouts.put("192.168.1.100", System.currentTimeMillis() - 1_000_000L);

        bruteForceFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        // Проверяем, что счетчик сброшен после истечения блокировки
        assertThat(failedAttempts.get("192.168.1.100").get()).isEqualTo(0);
        assertThat(lockouts).doesNotContainKey("192.168.1.100");
    }

    @Test
    void testDoFilter_GetRequestToLogin_SkipsCheck() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getMethod()).thenReturn("GET"); // GET запрос, не POST

        // Даже с большим количеством неудачных попыток
        failedAttempts.put("192.168.1.100", new AtomicInteger(10));

        bruteForceFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void testDoFilter_NewIp_StartsWithZeroAttempts() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.200"); // Новый IP
        when(request.getMethod()).thenReturn("POST");

        bruteForceFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        // Проверяем, что для нового IP создан счетчик с 0
        assertThat(failedAttempts.get("192.168.1.200").get()).isEqualTo(0);
    }
}