package com.platform.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import com.platform.util.XssSanitizer;

import static org.junit.jupiter.api.Assertions.*;
class XssFilterTest {

    private XssFilter xssFilter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        xssFilter = new XssFilter(new XssSanitizer());
        objectMapper = new ObjectMapper();
    }

    // ==================== POST/PUT — application/json ====================

    @Test
    void postWithScriptTag_Sanitized() throws Exception {
        String body = "{\"content\": \"<script>alert('xss')</script>Hello\"}";
        MockHttpServletRequest request = createJsonRequest("POST", "/api/posts", body);
        CapturingFilterChain chain = new CapturingFilterChain();

        xssFilter.doFilter(request, new MockHttpServletResponse(), chain);

        String sanitized = chain.getCapturedBody();
        assertTrue(sanitized.contains("&lt;script&gt;"), "Script tag should be escaped");
        assertFalse(sanitized.contains("<script>"), "Raw script tag should not remain");
        assertTrue(sanitized.contains("Hello"), "Normal text should be preserved");
    }

    @Test
    void putWithHtmlTags_Sanitized() throws Exception {
        String body = "{\"content\": \"<b>bold</b> <img src=x onerror=alert(1)>\"}";
        MockHttpServletRequest request = createJsonRequest("PUT", "/api/posts/123", body);
        CapturingFilterChain chain = new CapturingFilterChain();

        xssFilter.doFilter(request, new MockHttpServletResponse(), chain);

        String sanitized = chain.getCapturedBody();
        assertTrue(sanitized.contains("&lt;b&gt;bold&lt;/b&gt;"), "HTML tags should be escaped");
        assertFalse(sanitized.contains("<img"), "Raw img tag should not remain");
    }

    @Test
    void multipleFields_AllSanitized() throws Exception {
        String body = "{\"title\": \"<script>X</script>\", \"content\": \"Hello <b>world</b>\"}";
        MockHttpServletRequest request = createJsonRequest("POST", "/api/events/materials", body);
        CapturingFilterChain chain = new CapturingFilterChain();

        xssFilter.doFilter(request, new MockHttpServletResponse(), chain);

        String sanitized = chain.getCapturedBody();
        assertFalse(sanitized.contains("<script>"), "title should be sanitized");
        assertFalse(sanitized.contains("<b>"), "content should be sanitized");
    }

    @Test
    void nestedObject_Sanitized() throws Exception {
        String body = "{\"data\": {\"content\": \"<script>X</script>\"}}";
        MockHttpServletRequest request = createJsonRequest("POST", "/api/chat", body);
        CapturingFilterChain chain = new CapturingFilterChain();

        xssFilter.doFilter(request, new MockHttpServletResponse(), chain);

        String sanitized = chain.getCapturedBody();
        assertFalse(sanitized.contains("<script>"), "Nested content should be sanitized");
    }

    @Test
    void arrayElements_Sanitized() throws Exception {
        String body = "{\"tags\": [\"safe\", \"<script>X</script>\", \"ok\"]}";
        MockHttpServletRequest request = createJsonRequest("POST", "/api/posts", body);
        CapturingFilterChain chain = new CapturingFilterChain();

        xssFilter.doFilter(request, new MockHttpServletResponse(), chain);

        String sanitized = chain.getCapturedBody();
        assertFalse(sanitized.contains("<script>"), "Array elements should be sanitized");
        assertTrue(sanitized.contains("safe"), "Safe strings should be preserved");
    }

    @Test
    void quotesEscaped() throws Exception {
        String body = "{\"content\": \"He said \\\"hello\\\"\"}";
        MockHttpServletRequest request = createJsonRequest("POST", "/api/posts", body);
        CapturingFilterChain chain = new CapturingFilterChain();

        xssFilter.doFilter(request, new MockHttpServletResponse(), chain);

        String sanitized = chain.getCapturedBody();
        assertTrue(sanitized.contains("&quot;"), "Double quotes should be escaped");
    }

    @Test
    void russianText_Preserved() throws Exception {
        String body = "{\"content\": \"Привет, мир! Тест кириллицы\"}";
        MockHttpServletRequest request = createJsonRequest("POST", "/api/posts", body);
        CapturingFilterChain chain = new CapturingFilterChain();

        xssFilter.doFilter(request, new MockHttpServletResponse(), chain);

        String sanitized = chain.getCapturedBody();
        assertTrue(sanitized.contains("Привет, мир!"), "Russian text should be preserved");
        assertTrue(sanitized.contains("Тест кириллицы"), "Cyrillic text should be preserved");
    }

    // ==================== PASS-THROUGH cases ====================

    @Test
    void getRequests_PassThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/posts");
        CapturingFilterChain chain = new CapturingFilterChain();

        xssFilter.doFilter(request, new MockHttpServletResponse(), chain);

        assertTrue(chain.wasCalled(), "Filter chain should be called");
        assertNull(chain.getCapturedBody(), "GET requests should not be intercepted");
    }

    @Test
    void deleteRequests_PassThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/posts/123");
        CapturingFilterChain chain = new CapturingFilterChain();

        xssFilter.doFilter(request, new MockHttpServletResponse(), chain);

        assertTrue(chain.wasCalled(), "Filter chain should be called");
        assertNull(chain.getCapturedBody(), "DELETE requests should not be intercepted");
    }

    @Test
    void nonJsonContentType_PassThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/media/upload");
        request.setContentType("multipart/form-data");
        CapturingFilterChain chain = new CapturingFilterChain();

        xssFilter.doFilter(request, new MockHttpServletResponse(), chain);

        assertTrue(chain.wasCalled(), "Filter chain should be called");
        assertNull(chain.getCapturedBody(), "Non-JSON should pass through");
    }

    @Test
    void emptyBody_PassThrough() throws Exception {
        MockHttpServletRequest request = createJsonRequest("POST", "/api/posts", "");
        CapturingFilterChain chain = new CapturingFilterChain();

        xssFilter.doFilter(request, new MockHttpServletResponse(), chain);

        assertTrue(chain.wasCalled(), "Filter chain should be called");
        assertNull(chain.getCapturedBody(), "Empty body should pass through");
    }

    @Test
    void invalidJson_PassThrough() throws Exception {
        String body = "this is not json at all {{{";
        MockHttpServletRequest request = createJsonRequest("POST", "/api/posts", body);
        CapturingFilterChain chain = new CapturingFilterChain();

        xssFilter.doFilter(request, new MockHttpServletResponse(), chain);

        assertTrue(chain.wasCalled(), "Filter chain should be called");
        assertNull(chain.getCapturedBody(), "Invalid JSON should pass through");
    }

    // ==================== EDGE cases ====================

    @Test
    void nullFieldInJson_Preserved() throws Exception {
        String body = "{\"content\": \"test\", \"mediaUrl\": null}";
        MockHttpServletRequest request = createJsonRequest("POST", "/api/posts", body);
        CapturingFilterChain chain = new CapturingFilterChain();

        xssFilter.doFilter(request, new MockHttpServletResponse(), chain);

        String sanitized = chain.getCapturedBody();
        assertTrue(sanitized.contains("test"), "String field should be preserved");
        assertTrue(sanitized.contains("null"), "Null fields should be preserved");
    }

    @Test
    void numericFields_Preserved() throws Exception {
        String body = "{\"content\": \"Hello\", \"sortOrder\": 42, \"isActive\": true}";
        MockHttpServletRequest request = createJsonRequest("POST", "/api/materials", body);
        CapturingFilterChain chain = new CapturingFilterChain();

        xssFilter.doFilter(request, new MockHttpServletResponse(), chain);

        String sanitized = chain.getCapturedBody();
        assertTrue(sanitized.contains("42"), "Numeric fields should be preserved");
        assertTrue(sanitized.contains("true"), "Boolean fields should be preserved");
    }

    @Test
    void deeplyNestedObjects_AllSanitized() throws Exception {
        String body = "{\"level1\": {\"level2\": {\"content\": \"<script>X</script>\"}}}";
        MockHttpServletRequest request = createJsonRequest("POST", "/api/test", body);
        CapturingFilterChain chain = new CapturingFilterChain();

        xssFilter.doFilter(request, new MockHttpServletResponse(), chain);

        String sanitized = chain.getCapturedBody();
        assertFalse(sanitized.contains("<script>"), "Deeply nested content should be sanitized");
    }

    @Test
    void questionField_Sanitized() throws Exception {
        String body = "{\"question\": \"Что такое <script>alert(1)</script> питание?\"}";
        MockHttpServletRequest request = createJsonRequest("POST", "/api/chat/questions-dto/nutrition", body);
        CapturingFilterChain chain = new CapturingFilterChain();

        xssFilter.doFilter(request, new MockHttpServletResponse(), chain);

        String sanitized = chain.getCapturedBody();
        assertFalse(sanitized.contains("<script>"), "Question should be sanitized");
        assertTrue(sanitized.contains("питание?"), "Russian text in question should be preserved");
    }

    // ==================== HELPERS ====================

    private MockHttpServletRequest createJsonRequest(String method, String uri, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setContentType("application/json");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    /**
     * FilterChain, который перехватывает body из CachedBodyHttpServletRequest.
     */
    private static class CapturingFilterChain implements FilterChain {
        private boolean called = false;
        private String capturedBody;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
            called = true;
            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                try {
                    byte[] bytes = httpRequest.getInputStream().readAllBytes();
                    if (bytes.length > 0) {
                        capturedBody = new String(bytes, StandardCharsets.UTF_8);
                    }
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        boolean wasCalled() { return called; }
        String getCapturedBody() { return capturedBody; }
    }
}
