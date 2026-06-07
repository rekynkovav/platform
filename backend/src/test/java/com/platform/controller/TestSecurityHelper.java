package com.platform.controller;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.IOException;

/**
 * Helper для standaloneSetup MockMvc тестов с @AuthenticationPrincipal Jwt.
 *
 * В standaloneSetup без Spring-контекста нет springSecurityFilterChain,
 * поэтому AuthenticationPrincipalArgumentResolver не может получить
 * Authentication из SecurityContextHolder.
 *
 * Этот helper предоставляет:
 * 1. SecurityContextFilter — устанавливает Authentication в SecurityContextHolder
 *    перед вызовом контроллера и очищает после.
 * 2. withAuth() — RequestPostProcessor, который передаёт Authentication в filter
 *    через request attribute.
 *
 * Использование:
 *   mockMvc = MockMvcBuilders.standaloneSetup(controller)
 *       .addFilters(TestSecurityHelper.securityContextFilter())
 *       .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
 *       .setControllerAdvice(new GlobalExceptionHandler())
 *       .build();
 *
 *   mockMvc.perform(get("/api/...").with(TestSecurityHelper.withAuth(auth)))...
 */
public final class TestSecurityHelper {

    private TestSecurityHelper() {}

    public static Filter securityContextFilter() {
        return new SecurityContextFilter();
    }

    public static RequestPostProcessor withAuth(Authentication authentication) {
        return request -> {
            request.setAttribute("__SPRING_SECURITY_AUTHENTICATION", authentication);
            return request;
        };
    }

    private static class SecurityContextFilter implements Filter {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            Object authAttr = request.getAttribute("__SPRING_SECURITY_AUTHENTICATION");
            if (authAttr instanceof Authentication) {
                SecurityContextHolder.getContext().setAuthentication((Authentication) authAttr);
            }
            try {
                chain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }
}
