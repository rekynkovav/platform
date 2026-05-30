package com.platform.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BruteForceProtectionFilterTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private BruteForceProtectionFilter filter;
    private StringWriter responseWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        filter = new BruteForceProtectionFilter(redisTemplate);

        responseWriter = new StringWriter();
        printWriter = new PrintWriter(responseWriter);
        when(response.getWriter()).thenReturn(printWriter);
    }

    @Test
    void doFilter_LoginWithCorrectPassword_ResetsCounter() throws Exception {
        // Arrange
        String clientIp = "192.168.1.100";
        String key = "bruteforce:login:" + clientIp;

        when(request.getRemoteAddr()).thenReturn(clientIp);
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getParameter("username")).thenReturn("testuser");
        when(request.getParameter("password")).thenReturn("correctpassword");

        // Simulate previous failed attempts
        when(valueOperations.get(key)).thenReturn("3"); // 3 failed attempts

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response); // Request passes
        verify(valueOperations).delete(key); // Counter should be reset
        verify(valueOperations, never()).increment(key); // No increment for success
        verify(response, never()).setStatus(429); // No 429 error
    }

    @Test
    void doFilter_FiveFailedAttempts_BlocksFor15Minutes() throws Exception {
        // Arrange
        String clientIp = "192.168.1.101";
        String key = "bruteforce:login:" + clientIp;

        when(request.getRemoteAddr()).thenReturn(clientIp);
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getParameter("username")).thenReturn("testuser");
        when(request.getParameter("password")).thenReturn("wrongpassword");

        // Simulate 5 failed attempts (>= limit)
        when(valueOperations.get(key)).thenReturn("5");

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain, never()).doFilter(request, response); // Request blocked
        verify(response).setStatus(429); // 429 Too Many Requests
        verify(response).setContentType("application/json");

        // Check response body
        printWriter.flush();
        String responseBody = responseWriter.toString();
        assertThat(responseBody).contains("Too many login attempts");
        assertThat(responseBody).contains("blocked");
    }

    @Test
    void doFilter_BelowLimit_PassesThrough() throws Exception {
        // Arrange
        String clientIp = "192.168.1.102";
        String key = "bruteforce:login:" + clientIp;

        when(request.getRemoteAddr()).thenReturn(clientIp);
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getParameter("username")).thenReturn("testuser");
        when(request.getParameter("password")).thenReturn("wrongpassword");

        // Simulate 3 failed attempts (< limit of 5)
        when(valueOperations.get(key)).thenReturn("3");

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response); // Request passes
        verify(valueOperations).increment(key); // Counter incremented
        verify(valueOperations).expire(eq(key), eq(15L), eq(TimeUnit.MINUTES)); // 15 minute expiry
        verify(response, never()).setStatus(429); // No 429 error
    }

    @Test
    void doFilter_NotLoginEndpoint_SkipsProtection() throws Exception {
        // Arrange
        String clientIp = "192.168.1.103";

        when(request.getRemoteAddr()).thenReturn(clientIp);
        when(request.getRequestURI()).thenReturn("/api/users/profile"); // Not /login
        when(request.getMethod()).thenReturn("GET");

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response); // Request passes
        verify(valueOperations, never()).get(anyString()); // No Redis operations
        verify(valueOperations, never()).increment(anyString());
        verify(valueOperations, never()).delete(anyString());
    }

    @Test
    void doFilter_GETRequestToLogin_SkipsProtection() throws Exception {
        // Arrange
        String clientIp = "192.168.1.104";

        when(request.getRemoteAddr()).thenReturn(clientIp);
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getMethod()).thenReturn("GET"); // GET request, not POST

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response); // Request passes
        verify(valueOperations, never()).get(anyString()); // No Redis operations
        verify(valueOperations, never()).increment(anyString());
        verify(valueOperations, never()).delete(anyString());
    }

    @Test
    void doFilter_LoginSuccessAfterBlock_ResetsCounter() throws Exception {
        // Arrange
        String clientIp = "192.168.1.105";
        String key = "bruteforce:login:" + clientIp;

        when(request.getRemoteAddr()).thenReturn(clientIp);
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getParameter("username")).thenReturn("testuser");
        when(request.getParameter("password")).thenReturn("correctpassword");

        // Simulate blocked state (5 failed attempts)
        when(valueOperations.get(key)).thenReturn("5");

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response); // Request passes (successful login)
        verify(valueOperations).delete(key); // Counter should be deleted
        verify(valueOperations, never()).increment(key); // No increment for success
        verify(response, never()).setStatus(429); // No 429 error
    }

    @Test
    void doFilter_NoPasswordParameter_TreatedAsFailedAttempt() throws Exception {
        // Arrange
        String clientIp = "192.168.1.106";
        String key = "bruteforce:login:" + clientIp;

        when(request.getRemoteAddr()).thenReturn(clientIp);
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getParameter("username")).thenReturn("testuser");
        when(request.getParameter("password")).thenReturn(null); // No password

        // Simulate 2 previous failed attempts
        when(valueOperations.get(key)).thenReturn("2");

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response); // Request passes (to actual auth)
        verify(valueOperations).increment(key); // Counter should be incremented (treated as failed)
        verify(valueOperations).expire(eq(key), eq(15L), eq(TimeUnit.MINUTES));
    }

    @Test
    void doFilter_EmptyPassword_TreatedAsFailedAttempt() throws Exception {
        // Arrange
        String clientIp = "192.168.1.107";
        String key = "bruteforce:login:" + clientIp;

        when(request.getRemoteAddr()).thenReturn(clientIp);
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getParameter("username")).thenReturn("testuser");
        when(request.getParameter("password")).thenReturn(""); // Empty password

        // Simulate 1 previous failed attempt
        when(valueOperations.get(key)).thenReturn("1");

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response); // Request passes (to actual auth)
        verify(valueOperations).increment(key); // Counter should be incremented
        verify(valueOperations).expire(eq(key), eq(15L), eq(TimeUnit.MINUTES));
    }
}