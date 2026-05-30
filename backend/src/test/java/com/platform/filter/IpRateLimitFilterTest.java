package com.platform.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.config.RateLimitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IpRateLimitFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private IpRateLimitFilter filter;
    private ObjectMapper objectMapper;
    private ConcurrentHashMap<String, AtomicInteger> rateLimitCache;
    private StringWriter responseWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        rateLimitCache = new ConcurrentHashMap<>();
        filter = new IpRateLimitFilter(objectMapper, rateLimitCache);

        responseWriter = new StringWriter();
        printWriter = new PrintWriter(responseWriter);
        when(response.getWriter()).thenReturn(printWriter);
    }

    @Test
    void doFilter_AuthEndpoint_BelowLimit_Passes() throws Exception {
        // Arrange
        String clientIp = "192.168.1.100";
        String path = "/api/auth/login";

        when(request.getRemoteAddr()).thenReturn(clientIp);
        when(request.getRequestURI()).thenReturn(path);
        when(request.getMethod()).thenReturn("POST");

        // Act - Make 9 requests (below limit of 10)
        for (int i = 0; i < 9; i++) {
            filter.doFilter(request, response, filterChain);
        }

        // Assert
        verify(filterChain, times(9)).doFilter(request, response);
        verify(response, never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void doFilter_AuthEndpoint_ExceedsLimit_Returns429() throws Exception {
        // Arrange
        String clientIp = "192.168.1.101";
        String path = "/api/auth/login";

        when(request.getRemoteAddr()).thenReturn(clientIp);
        when(request.getRequestURI()).thenReturn(path);
        when(request.getMethod()).thenReturn("POST");

        // Act - Make 11 requests (exceeds limit of 10)
        for (int i = 0; i < 11; i++) {
            filter.doFilter(request, response, filterChain);
        }

        // Assert
        verify(filterChain, times(10)).doFilter(request, response); // First 10 pass
        verify(response, times(1)).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response, times(1)).setContentType("application/json");

        // Check response body
        printWriter.flush();
        String responseBody = responseWriter.toString();
        assertThat(responseBody).contains("Too many requests");
        assertThat(responseBody).contains("rate limit exceeded");
    }

    @Test
    void doFilter_PaymentsEndpoint_HigherLimit() throws Exception {
        // Arrange
        String clientIp = "192.168.1.102";
        String path = "/api/payments/create";

        when(request.getRemoteAddr()).thenReturn(clientIp);
        when(request.getRequestURI()).thenReturn(path);
        when(request.getMethod()).thenReturn("POST");

        // Act - Make 30 requests (at limit for payments)
        for (int i = 0; i < 30; i++) {
            filter.doFilter(request, response, filterChain);
        }

        // Assert - All 30 should pass
        verify(filterChain, times(30)).doFilter(request, response);
        verify(response, never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void doFilter_HealthcheckEndpoint_NoRateLimit() throws Exception {
        // Arrange
        String clientIp = "192.168.1.103";
        String path = "/actuator/health";

        when(request.getRemoteAddr()).thenReturn(clientIp);
        when(request.getRequestURI()).thenReturn(path);
        when(request.getMethod()).thenReturn("GET");

        // Act - Make 1000 requests (should all pass)
        for (int i = 0; i < 1000; i++) {
            filter.doFilter(request, response, filterChain);
        }

        // Assert
        verify(filterChain, times(1000)).doFilter(request, response);
        verify(response, never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(rateLimitCache).isEmpty(); // No entries should be created
    }

    @Test
    void doFilter_XForwardedFor_ParsesFirstIp() throws Exception {
        // Arrange
        String xForwardedFor = "203.0.113.195, 198.51.100.100, 192.0.2.1";
        String expectedIp = "203.0.113.195"; // First IP
        String path = "/api/auth/login";

        when(request.getHeader("X-Forwarded-For")).thenReturn(xForwardedFor);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1"); // Should be ignored
        when(request.getRequestURI()).thenReturn(path);
        when(request.getMethod()).thenReturn("POST");

        // Act - Make requests
        for (int i = 0; i < 11; i++) {
            filter.doFilter(request, response, filterChain);
        }

        // Assert
        String cacheKey = expectedIp + ":" + path;
        assertThat(rateLimitCache).containsKey(cacheKey);
        verify(response, times(1)).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void doFilter_XForwardedFor_Unknown_Ignores() throws Exception {
        // Arrange
        String xForwardedFor = "unknown, 198.51.100.100";
        String expectedIp = "198.51.100.100"; // Second IP (first is "unknown")
        String path = "/api/auth/login";

        when(request.getHeader("X-Forwarded-For")).thenReturn(xForwardedFor);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getRequestURI()).thenReturn(path);
        when(request.getMethod()).thenReturn("POST");

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        String cacheKey = expectedIp + ":" + path;
        assertThat(rateLimitCache).containsKey(cacheKey);
    }

    @Test
    void doFilter_AddsRateLimitHeaders() throws Exception {
        // Arrange
        String clientIp = "192.168.1.104";
        String path = "/api/auth/login";

        when(request.getRemoteAddr()).thenReturn(clientIp);
        when(request.getRequestURI()).thenReturn(path);
        when(request.getMethod()).thenReturn("POST");

        // Act - Make 5 requests
        for (int i = 0; i < 5; i++) {
            filter.doFilter(request, response, filterChain);
        }

        // Assert - Headers should be added
        verify(response, times(5)).setHeader("X-RateLimit-Limit", "10");
        verify(response, atLeastOnce()).setHeader(eq("X-RateLimit-Remaining"), anyString());
        verify(response, atLeastOnce()).setHeader(eq("X-RateLimit-Reset"), anyString());
    }

    @Test
    void doFilter_DifferentPaths_DifferentCounters() throws Exception {
        // Arrange
        String clientIp = "192.168.1.105";

        when(request.getRemoteAddr()).thenReturn(clientIp);
        when(request.getMethod()).thenReturn("POST");

        // Act - Make requests to different paths
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        filter.doFilter(request, response, filterChain); // 1st auth request

        when(request.getRequestURI()).thenReturn("/api/payments/create");
        filter.doFilter(request, response, filterChain); // 1st payment request

        when(request.getRequestURI()).thenReturn("/api/chat/send");
        filter.doFilter(request, response, filterChain); // 1st chat request

        // Assert - Different cache keys should exist
        assertThat(rateLimitCache).containsKey(clientIp + ":/api/auth/login");
        assertThat(rateLimitCache).containsKey(clientIp + ":/api/payments/create");
        assertThat(rateLimitCache).containsKey(clientIp + ":/api/chat/send");

        // Each should have count = 1
        assertThat(rateLimitCache.get(clientIp + ":/api/auth/login").get()).isEqualTo(1);
        assertThat(rateLimitCache.get(clientIp + ":/api/payments/create").get()).isEqualTo(1);
        assertThat(rateLimitCache.get(clientIp + ":/api/chat/send").get()).isEqualTo(1);
    }

    @Test
    void doFilter_GETRequest_NoRateLimitForSomePaths() throws Exception {
        // Arrange
        String clientIp = "192.168.1.106";
        String path = "/api/users/profile";

        when(request.getRemoteAddr()).thenReturn(clientIp);
        when(request.getRequestURI()).thenReturn(path);
        when(request.getMethod()).thenReturn("GET");

        // Act - Make 150 requests (exceeds general API limit of 100)
        for (int i = 0; i < 150; i++) {
            filter.doFilter(request, response, filterChain);
        }

        // Assert - All should pass (GET requests might have different limits or no limits)
        verify(filterChain, times(150)).doFilter(request, response);
        // Note: Actual behavior depends on filter implementation
    }
}