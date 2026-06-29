package com.platform.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.platform.util.XssSanitizer;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * XSS Filter — автоматически санитизирует все строковые поля
 * в JSON body POST/PUT запросов.
 *
 * Использует XssSanitizer дляescaping HTML-сущностей (&, <, >, ", ').
 * Работает на уровне JSON дерева — рекурсивно обходит все string-ноды.
 *
 * Порядок: Order(2) — после IpRateLimitFilter (по умолчанию Order(0)).
 * Не затрагивает: multipart, form-urlencoded, GET/DELETE запросы.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class XssFilter implements Filter {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final XssSanitizer xssSanitizer;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String method = httpRequest.getMethod();
        String contentType = httpRequest.getContentType();

        // Санитизируем только POST/PUT с JSON body
        if (("POST".equals(method) || "PUT".equals(method))
                && contentType != null && contentType.contains("application/json")) {

            // Читаем body
            byte[] bodyBytes = readRequestBody(httpRequest);
            if (bodyBytes == null || bodyBytes.length == 0) {
                chain.doFilter(request, response);
                return;
            }

            String body = new String(bodyBytes, StandardCharsets.UTF_8);
            if (body.isBlank()) {
                chain.doFilter(request, response);
                return;
            }

            try {
                // Парсим JSON
                JsonNode jsonNode = objectMapper.readTree(body);

                // Рекурсивно санитизируем
                sanitizeNode(jsonNode);

                // Конвертируем обратно в строку
                String sanitizedBody = objectMapper.writeValueAsString(jsonNode);

                // Создаём wrapper с перезаписанным body
                CachedBodyHttpServletRequest cachedRequest =
                        new CachedBodyHttpServletRequest(httpRequest, sanitizedBody.getBytes(StandardCharsets.UTF_8));

                log.debug("XSS filter sanitized request body for {} {}", method, httpRequest.getRequestURI());
                chain.doFilter(cachedRequest, response);

            } catch (Exception e) {
                // Если JSON невалидный — пропускаем без санитизации
                // (валидация DTO поймает ошибку дальше)
                log.debug("XSS filter: non-JSON body for {} {}, passing through",
                        method, httpRequest.getRequestURI());
                chain.doFilter(request, response);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     * Рекурсивно санитизирует все строковые ноды в JSON дереве.
     */
    private void sanitizeNode(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            var fieldNames = objectNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode child = objectNode.get(fieldName);
                if (child.isTextual()) {
                    String sanitized = xssSanitizer.sanitize(child.asText());
                    objectNode.put(fieldName, sanitized);
                } else {
                    sanitizeNode(child);
                }
            }
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode child = arrayNode.get(i);
                if (child.isTextual()) {
                    String sanitized = xssSanitizer.sanitize(child.asText());
                    arrayNode.set(i, objectMapper.getNodeFactory().textNode(sanitized));
                } else {
                    sanitizeNode(child);
                }
            }
        }
    }

    private byte[] readRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("XSS Filter initialized");
    }

    @Override
    public void destroy() {
        log.info("XSS Filter destroyed");
    }

    /**
     * HttpServletRequestWrapper, который возвращает перезаписанный body.
     */
    private static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        CachedBodyHttpServletRequest(HttpServletRequest request, byte[] cachedBody) {
            super(request);
            this.cachedBody = cachedBody;
        }

        @Override
        public ServletInputStream getInputStream() {
            return new ServletInputStream() {
                private final ByteArrayInputStream byteArrayInputStream =
                        new ByteArrayInputStream(cachedBody);

                @Override
                public int read() {
                    return byteArrayInputStream.read();
                }

                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    // no-op
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);
            return new BufferedReader(new InputStreamReader(byteArrayInputStream, StandardCharsets.UTF_8));
        }
    }
}
