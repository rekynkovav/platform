package com.platform.util;

import com.platform.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
class UtilClassesTest {

    // ==================== XssSanitizer ====================

    @Test
    void xssSanitizer_ScriptTag_Escaped() {
        XssSanitizer sanitizer = new XssSanitizer();
        String result = sanitizer.sanitize("<script>alert('xss')</script>Hello");
        assertFalse(result.contains("<script>"));
        assertTrue(result.contains("&lt;script&gt;"));
    }

    @Test
    void xssSanitizer_Quotes_Escaped() {
        XssSanitizer sanitizer = new XssSanitizer();
        String result = sanitizer.sanitize("\"quoted\"");
        assertTrue(result.contains("&quot;"));
    }

    @Test
    void xssSanitizer_SingleQuote_Escaped() {
        XssSanitizer sanitizer = new XssSanitizer();
        String result = sanitizer.sanitize("it's");
        assertTrue(result.contains("&#x27;"));
    }

    @Test
    void xssSanitizer_NullAndEmpty_HandledCorrectly() {
        XssSanitizer sanitizer = new XssSanitizer();
        assertNull(sanitizer.sanitize(null));
        assertEquals("", sanitizer.sanitize(""));
    }

    @Test
    void xssSanitizeForHtml_DelegatesToSanitize() {
        XssSanitizer sanitizer = new XssSanitizer();
        String input = "<b>bold</b>";
        assertEquals(sanitizer.sanitize(input), sanitizer.sanitizeForHtml(input));
    }

    // ==================== ValidationUtils ====================

    @Test
    void validationUtils_RequireNonNull_NullThrows() {
        assertThrows(ValidationException.class, () ->
                ValidationUtils.requireNonNull(null, "Required"));
    }

    @Test
    void validationUtils_RequireNonNull_NotNull_DoesNotThrow() {
        assertDoesNotThrow(() ->
                ValidationUtils.requireNonNull("value", "Required"));
    }

    @Test
    void validationUtils_RequireNotBlank_BlankThrows() {
        assertThrows(ValidationException.class, () ->
                ValidationUtils.requireNotBlank("   ", "Name required"));
        assertThrows(ValidationException.class, () ->
                ValidationUtils.requireNotBlank(null, "Name required"));
    }

    @Test
    void validationUtils_ValidateId_ValidUuid_DoesNotThrow() {
        assertDoesNotThrow(() ->
                ValidationUtils.validateId("550e8400-e29b-41d4-a716-446655440000", "userId"));
    }

    @Test
    void validationUtils_ValidateId_InvalidFormat_Throws() {
        assertThrows(ValidationException.class, () ->
                ValidationUtils.validateId("not-a-uuid", "userId"));
    }

    @Test
    void validationUtils_ValidateId_Blank_Throws() {
        assertThrows(ValidationException.class, () ->
                ValidationUtils.validateId("", "userId"));
    }
}
