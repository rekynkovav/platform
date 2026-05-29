package com.platform.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class XssSanitizerTest {

    @Test
    void testSanitize_NullInput_ReturnsNull() {
        assertThat(XssSanitizer.sanitize(null)).isNull();
    }

    @Test
    void testSanitize_EmptyString_ReturnsEmpty() {
        assertThat(XssSanitizer.sanitize("")).isEmpty();
    }

    @Test
    void testSanitize_PlainText_ReturnsSame() {
        String input = "This is plain text without any HTML";
        assertThat(XssSanitizer.sanitize(input)).isEqualTo(input);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "<script>alert('xss')</script>",
            "<script src='http://evil.com/xss.js'></script>",
            "<script>console.log('xss')</script>",
            "<script type='text/javascript'>alert(1)</script>"
    })
    void testSanitize_ScriptTags_AreEscaped(String input) {
        String result = XssSanitizer.sanitize(input);
        assertThat(result).doesNotContain("<script>");
        assertThat(result).doesNotContain("</script>");
        assertThat(result).contains("&lt;script&gt;");
        assertThat(result).contains("&lt;/script&gt;");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "<b>bold</b>",
            "<i>italic</i>",
            "<u>underline</u>",
            "<strong>strong</strong>",
            "<em>emphasis</em>"
    })
    void testSanitize_SafeHtmlTags_ArePreserved(String input) {
        String result = XssSanitizer.sanitize(input);
        assertThat(result).isEqualTo(input); // Безопасные теги сохраняются
    }

    @Test
    void testSanitize_HrefWithJavaScript_IsBlocked() {
        String input = "<a href=\"javascript:alert('xss')\">click me</a>";
        String result = XssSanitizer.sanitize(input);

        assertThat(result).doesNotContain("javascript:");
        assertThat(result).contains("href=\"#\"");
        assertThat(result).contains("click me");
    }

    @Test
    void testSanitize_HttpsLink_IsPreservedWithRel() {
        String input = "<a href=\"https://example.com\">safe link</a>";
        String result = XssSanitizer.sanitize(input);

        assertThat(result).contains("href=\"https://example.com\"");
        assertThat(result).contains("rel=\"noopener noreferrer\"");
        assertThat(result).contains("safe link");
    }

    @Test
    void testSanitize_HttpLink_IsPreservedWithRel() {
        String input = "<a href=\"http://example.com\">http link</a>";
        String result = XssSanitizer.sanitize(input);

        assertThat(result).contains("href=\"http://example.com\"");
        assertThat(result).contains("rel=\"noopener noreferrer\"");
    }

    @Test
    void testSanitize_OnClickAttribute_IsRemoved() {
        String input = "<div onclick=\"alert('xss')\">clickable</div>";
        String result = XssSanitizer.sanitize(input);

        assertThat(result).doesNotContain("onclick");
        assertThat(result).contains("<div>clickable</div>");
    }

    @Test
    void testSanitize_OnMouseOverAttribute_IsRemoved() {
        String input = "<span onmouseover=\"alert('xss')\">hover me</span>";
        String result = XssSanitizer.sanitize(input);

        assertThat(result).doesNotContain("onmouseover");
        assertThat(result).contains("<span>hover me</span>");
    }

    @Test
    void testSanitize_ImgWithOnError_IsSanitized() {
        String input = "<img src=\"x\" onerror=\"alert('xss')\" />";
        String result = XssSanitizer.sanitize(input);

        assertThat(result).doesNotContain("onerror");
        assertThat(result).contains("<img src=\"x\" />");
    }

    @Test
    void testSanitize_ComplexXss_IsFullySanitized() {
        String input = """
            <script>alert(1)</script>
            <b>bold text</b>
            <a href="javascript:alert(2)">bad link</a>
            <div onclick="alert(3)">clickable</div>
            <img src="x" onerror="alert(4)" />
            """;

        String result = XssSanitizer.sanitize(input);

        assertThat(result).doesNotContain("<script>");
        assertThat(result).doesNotContain("javascript:");
        assertThat(result).doesNotContain("onclick");
        assertThat(result).doesNotContain("onerror");
        assertThat(result).contains("<b>bold text</b>");
        assertThat(result).contains("bad link");
    }

    @Test
    void testSanitize_MixedContent_HandlesCorrectly() {
        String input = "Hello <b>world</b>! <script>alert('xss')</script> Visit <a href=\"https://example.com\">our site</a>";
        String result = XssSanitizer.sanitize(input);

        assertThat(result).contains("Hello <b>world</b>!");
        assertThat(result).doesNotContain("<script>");
        assertThat(result).contains("&lt;script&gt;");
        assertThat(result).contains("href=\"https://example.com\"");
        assertThat(result).contains("rel=\"noopener noreferrer\"");
        assertThat(result).contains("Visit");
        assertThat(result).contains("our site");
    }
}