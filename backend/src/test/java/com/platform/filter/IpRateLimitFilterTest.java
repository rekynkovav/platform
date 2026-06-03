package com.platform.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IpRateLimitFilterTest {

    private IpRateLimitFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new IpRateLimitFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
    }

    // ==================== Health/static bypass ====================

    @Test
    void doFilter_HealthCheck_BypassesRateLimit() throws Exception {
        when(request.getRequestURI()).thenReturn("/actuator/health");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void doFilter_SwaggerUi_BypassesRateLimit() throws Exception {
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_StaticCss_BypassesRateLimit() throws Exception {
        when(request.getRequestURI()).thenReturn("/static/style.css");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    // ==================== Rate limiting ====================

    @Test
    void doFilter_NormalApiRequest_AddsRateLimitHeaders() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response).setHeader(eq("X-RateLimit-Limit"), anyString());
        verify(response).setHeader(eq("X-RateLimit-Remaining"), anyString());
        verify(response).setHeader(eq("X-RateLimit-Reset"), anyString());
    }

    @Test
    void doFilter_AuthEndpoint_UsesStricterLimit() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        // Send 11 requests to exceed auth limit of 10
        for (int i = 0; i < 11; i++) {
            filter.doFilter(request, response, chain);
        }

        verify(response).setStatus(429);
    }

    // ==================== Client IP detection ====================

    @Test
    void doFilter_XForwardedFor_UsesFirstIp() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    // ==================== init / destroy ====================

    @Test
    void init_DoesNotThrow() {
        assertDoesNotThrow(() -> filter.init(null));
    }

    @Test
    void destroy_DoesNotThrow() {
        assertDoesNotThrow(() -> filter.destroy());
    }
}
