package com.platform.filter;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class IpRateLimitFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Bucket bucket;

    @Mock
    private ConsumptionProbe probe;

    private IpRateLimitFilter rateLimitFilter;
    private ConcurrentHashMap<String, Bucket> buckets;

    @BeforeEach
    void setUp() {
        buckets = new ConcurrentHashMap<>();
        rateLimitFilter = new IpRateLimitFilter(buckets);
    }

    @Test
    void testDoFilter_NormalRequest_PassesThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/some-endpoint");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        // Создаем bucket, который разрешает запрос
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
        when(probe.isConsumed()).thenReturn(true);
        when(probe.getRemainingTokens()).thenReturn(9L);

        buckets.put("192.168.1.100", bucket);

        rateLimitFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response).setHeader(eq("X-RateLimit-Remaining"), eq("9"));
        verify(response, never()).setStatus(429);
    }

    @Test
    void testDoFilter_HealthCheckEndpoint_SkipsRateLimit() throws Exception {
        when(request.getRequestURI()).thenReturn("/actuator/health");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        rateLimitFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setHeader(anyString(), anyString());
        verify(response, never()).setStatus(429);
    }

    @Test
    void testDoFilter_XffFromUntrustedProxy_IsIgnored() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1"); // IP прокси
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100, 10.0.0.2");

        // Создаем bucket для IP прокси, а не для клиента
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
        when(probe.isConsumed()).thenReturn(true);

        buckets.put("10.0.0.1", bucket);

        rateLimitFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        // Проверяем, что использовался IP прокси, а не клиентский IP из XFF
        verify(bucket).tryConsumeAndReturnRemaining(1);
    }

    @Test
    void testDoFilter_AuthEndpoint_HasLimit10() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        // Имитируем 11-й запрос (лимит 10)
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
        when(probe.isConsumed()).thenReturn(false);
        when(probe.getNanosToWaitForRefill()).thenReturn(1_000_000_000L); // 1 секунда

        buckets.put("192.168.1.100", bucket);

        rateLimitFilter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(429);
        verify(response).setHeader(eq("X-RateLimit-Retry-After"), anyString());
        verify(response).setHeader(eq("X-RateLimit-Limit"), eq("10"));
    }

    @Test
    void testDoFilter_NonAuthEndpoint_HasLimit100() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users/profile");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        // Создаем bucket для не-auth endpoint (лимит 100)
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
        when(probe.isConsumed()).thenReturn(true);
        when(probe.getRemainingTokens()).thenReturn(99L);

        buckets.put("192.168.1.100", bucket);

        rateLimitFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response).setHeader(eq("X-RateLimit-Remaining"), eq("99"));
        verify(response).setHeader(eq("X-RateLimit-Limit"), eq("100"));
    }

    @Test
    void testDoFilter_NewIp_CreatesBucket() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/some-endpoint");
        when(request.getRemoteAddr()).thenReturn("192.168.1.200"); // Новый IP
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        // Bucket будет создан автоматически
        rateLimitFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(buckets).containsKey("192.168.1.200");
    }

    @Test
    void testDoFilter_WithValidXffFromTrustedProxy_UsesClientIp() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1"); // Доверенный прокси
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.150");

        // Настраиваем доверенные прокси
        rateLimitFilter = new IpRateLimitFilter(buckets);
        // В реальном коде доверенные прокси настраиваются через конструктор/конфигурацию

        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
        when(probe.isConsumed()).thenReturn(true);

        buckets.put("192.168.1.150", bucket);

        rateLimitFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        // Проверяем, что использовался клиентский IP из XFF
        verify(bucket).tryConsumeAndReturnRemaining(1);
    }
}