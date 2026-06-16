package com.platform.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import com.platform.util.RoleUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtDecoder jwtDecoder;

    public WebSocketHandshakeInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        HttpHeaders headers = request.getHeaders();
        List<String> authHeaders = headers.get("Authorization");

        String token = null;
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
                log.debug("Token extracted from Authorization header");
            }
        }

        // Fallback: check query parameter for token (common for WebSocket connections)
        if (token == null) {
            String query = request.getURI().getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] kv = param.split("=", 2);
                    if (kv.length == 2 && "token".equals(kv[0])) {
                        token = kv[1];
                        log.debug("Token extracted from query parameter");
                        break;
                    }
                }
            }
        }

        if (token == null) {
            log.warn("No token provided in WebSocket handshake");
            return false;
        }

        try {
            Jwt jwt = jwtDecoder.decode(token);
            attributes.put("userId", jwt.getSubject());
            attributes.put("token", token);
            attributes.put("userRole", RoleUtils.getUserRole(jwt));

            log.debug("WebSocket authenticated: userId={}", jwt.getSubject());
            return true;

        } catch (JwtException e) {
            log.error("Invalid token in WebSocket handshake: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("WebSocket handshake failed", exception);
        }
    }
}
