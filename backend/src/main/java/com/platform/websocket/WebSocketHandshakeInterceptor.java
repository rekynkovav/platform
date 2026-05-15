package com.platform.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // Извлекаем токен из query параметров
        String query = request.getURI().getQuery();
        if (query != null && query.contains("token=")) {
            String token = query.split("token=")[1].split("&")[0];
            attributes.put("token", token);
            log.debug("Token extracted from query");
        }

        // Извлекаем userId из заголовков
        String userId = request.getHeaders().getFirst("X-User-Id");
        if (userId != null) {
            attributes.put("userId", userId);
            log.debug("UserId from header: {}", userId);
        } else {
            log.debug("No X-User-Id header found");
        }

        // Получаем principal из SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal != null) {
                attributes.put("userId", principal.getName());
                attributes.put("userRole", principal.getAuthorities().iterator().next().getAuthority());
            }
        }

        log.debug("WebSocket handshake from: {}", request.getRemoteAddress());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("WebSocket handshake failed", exception);
        } else {
            log.debug("WebSocket handshake successful");
        }
    }
}