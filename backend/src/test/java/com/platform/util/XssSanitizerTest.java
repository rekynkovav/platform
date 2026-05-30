package com.platform.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class XssSanitizerTest {

    private XssSanitizer xssSanitizer;

    @BeforeEach
    void setUp() {
        xssSanitizer = new XssSanitizer();
    }

    @Test
    void sanitize_ScriptTag_Removed() {
        // Arrange
        String input = "<script>alert('XSS')</script>Hello World";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("Hello World");
    }

    @Test
    void sanitize_ScriptTagWithAttributes_Removed() {
        // Arrange
        String input = "<script type=\"text/javascript\">alert('XSS')</script>Content";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("Content");
    }

    @Test
    void sanitize_ImgTagWithOnError_Removed() {
        // Arrange
        String input = "<img src=\"x\" onerror=\"alert('XSS')\">Image";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("Image");
    }

    @Test
    void sanitize_IframeTag_Removed() {
        // Arrange
        String input = "<iframe src=\"http://evil.com\"></iframe>Content";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("Content");
    }

    @Test
    void sanitize_JavaScriptProtocol_Removed() {
        // Arrange
        String input = "<a href=\"javascript:alert('XSS')\">Click me</a>";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("<a href=\"\">Click me</a>");
    }

    @Test
    void sanitize_DataProtocol_Removed() {
        // Arrange
        String input = "<a href=\"data:text/html,<script>alert('XSS')</script>\">Click</a>";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("<a href=\"\">Click</a>");
    }

    @Test
    void sanitize_OnEventAttributes_Removed() {
        // Arrange
        String input = "<div onclick=\"alert('XSS')\" onmouseover=\"alert('XSS2')\">Content</div>";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("<div>Content</div>");
    }

    @Test
    void sanitize_StyleTagWithExpression_Removed() {
        // Arrange
        String input = "<style>body { background: expression(alert('XSS')) }</style>Content";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("Content");
    }

    @Test
    void sanitize_SafeHtml_Preserved() {
        // Arrange
        String input = "<p>This is <strong>safe</strong> HTML with <a href=\"https://example.com\">link</a>.</p>";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("<p>This is <strong>safe</strong> HTML with <a href=\"https://example.com\">link</a>.</p>");
    }

    @Test
    void sanitize_EncodedScript_Removed() {
        // Arrange
        String input = "&lt;script&gt;alert('XSS')&lt;/script&gt;Hello";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("Hello");
    }

    @Test
    void sanitize_MixedCaseScriptTag_Removed() {
        // Arrange
        String input = "<ScRiPt>alert('XSS')</ScRiPt>Content";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("Content");
    }

    @Test
    void sanitize_ScriptInAttribute_Removed() {
        // Arrange
        String input = "<div style=\"background: url(javascript:alert('XSS'))\">Content</div>";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("<div>Content</div>");
    }

    @Test
    void sanitize_ObjectTag_Removed() {
        // Arrange
        String input = "<object data=\"evil.swf\"></object>Content";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("Content");
    }

    @Test
    void sanitize_EmbedTag_Removed() {
        // Arrange
        String input = "<embed src=\"evil.swf\">Content";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("Content");
    }

    @Test
    void sanitize_FormTag_Removed() {
        // Arrange
        String input = "<form action=\"evil.php\"><input type=\"text\"></form>Content";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("Content");
    }

    @Test
    void sanitize_InputTag_Removed() {
        // Arrange
        String input = "<input type=\"text\" onfocus=\"alert('XSS')\">Content";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("Content");
    }

    @Test
    void sanitize_TextareaTag_Removed() {
        // Arrange
        String input = "<textarea onchange=\"alert('XSS')\">Content</textarea>";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("");
    }

    @Test
    void sanitize_SelectTag_Removed() {
        // Arrange
        String input = "<select onchange=\"alert('XSS')\"><option>1</option></select>Content";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("Content");
    }

    @Test
    void sanitize_ButtonTag_Removed() {
        // Arrange
        String input = "<button onclick=\"alert('XSS')\">Click</button>Content";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("Content");
    }

    @Test
    void sanitize_SvgTag_Removed() {
        // Arrange
        String input = "<svg onload=\"alert('XSS')\"></svg>Content";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("Content");
    }

    @Test
    void sanitize_MathTag_Removed() {
        // Arrange
        String input = "<math><mi>x</mi></math>Content";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("Content");
    }

    @Test
    void sanitize_NullInput_ReturnsEmptyString() {
        // Arrange
        String input = null;

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("");
    }

    @Test
    void sanitize_EmptyString_ReturnsEmptyString() {
        // Arrange
        String input = "";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("");
    }

    @Test
    void sanitize_WhitespaceOnly_ReturnsSame() {
        // Arrange
        String input = "   \n\t  ";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("   \n\t  ");
    }

    @Test
    void sanitize_PlainText_ReturnsSame() {
        // Arrange
        String input = "This is plain text without any HTML tags.";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("This is plain text without any HTML tags.");
    }

    @Test
    void sanitize_AllowedTagsWithAttributes_Preserved() {
        // Arrange
        String input = "<p class=\"text-center\" id=\"main\">Paragraph with <a href=\"https://example.com\" target=\"_blank\" rel=\"noopener\">link</a> and <strong>bold</strong> text.</p>";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("<p class=\"text-center\" id=\"main\">Paragraph with <a href=\"https://example.com\" target=\"_blank\" rel=\"noopener\">link</a> and <strong>bold</strong> text.</p>");
    }

    @Test
    void sanitize_ComplexXssAttempt_Removed() {
        // Arrange
        String input = "<div><script>alert('XSS1')</script><img src=\"x\" onerror=\"alert('XSS2')\"><a href=\"javascript:alert('XSS3')\">Click</a></div>Safe content";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("<div>Safe content</div>");
    }

    @Test
    void sanitize_LineBreaks_Preserved() {
        // Arrange
        String input = "Line 1\nLine 2\r\nLine 3";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("Line 1\nLine 2\r\nLine 3");
    }

    @Test
    void sanitize_SpecialCharacters_Preserved() {
        // Arrange
        String input = "Text with &amp; &lt; &gt; &quot; &apos; symbols";

        // Act
        String result = xssSanitizer.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("Text with &amp; &lt; &gt; &quot; &apos; symbols");
    }
}