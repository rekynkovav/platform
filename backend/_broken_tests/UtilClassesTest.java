package com.platform.util;

import com.platform.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilClassesTest {

    // ==================== SqlValidator ====================

    @Test
    void sqlValidator_SafeInput_ReturnsTrue() {
        SqlValidator validator = new SqlValidator();
        assertTrue(validator.isSafe("Hello World"));
        assertTrue(validator.isSafe("user@example.com"));
        assertTrue(validator.isSafe(null));
        assertTrue(validator.isSafe(""));
    }

    @Test
    void sqlValidator_SqlInjection_ReturnsFalse() {
        SqlValidator validator = new SqlValidator();
        assertFalse(validator.isSafe("'; DROP TABLE users; --"));
        assertFalse(validator.isSafe("1 OR 1=1"));
        assertFalse(validator.isSafe("SELECT * FROM users"));
        assertFalse(validator.isSafe("UNION SELECT password FROM admin"));
    }

    @Test
    void sqlValidator_Sanitize_RemovesDangerousChars() {
        SqlValidator validator = new SqlValidator();
        String result = validator.sanitize("test'; DROP TABLE--");
        assertFalse(result.contains("'"));
        assertFalse(result.contains("--"));
    }

    @Test
    void sqlValidator_Sanitize_NullReturnsNull() {
        SqlValidator validator = new SqlValidator();
        assertNull(validator.sanitize(null));
    }

    @Test
    void sqlValidator_IsValidOrderBy_ValidColumn_ReturnsTrue() {
        SqlValidator validator = new SqlValidator();
        assertTrue(validator.isValidOrderBy("created_at"));
        assertTrue(validator.isValidOrderBy("username"));
        assertTrue(validator.isValidOrderBy(null));
        assertTrue(validator.isValidOrderBy(""));
    }

    @Test
    void sqlValidator_IsValidOrderBy_InjectionAttempt_ReturnsFalse() {
        SqlValidator validator = new SqlValidator();
        assertFalse(validator.isValidOrderBy("id; DROP TABLE"));
        assertFalse(validator.isValidOrderBy("1=1"));
    }

    // ==================== XssSanitizer ====================

    @Test
    void xssSanitizer_ScriptTag_Removed() {
        XssSanitizer sanitizer = new XssSanitizer();
        String result = sanitizer.sanitize("<script>alert('xss')</script>Hello");
        assertFalse(result.toLowerCase().contains("<script>"));
    }

    @Test
    void xssSanitizer_JavascriptProtocol_Removed() {
        XssSanitizer sanitizer = new XssSanitizer();
        String result = sanitizer.sanitize("javascript:alert(1)");
        assertFalse(result.toLowerCase().contains("javascript:"));
    }

    @Test
    void xssSanitizer_EventHandlers_Removed() {
        XssSanitizer sanitizer = new XssSanitizer();
        String result = sanitizer.sanitize("<img onerror=alert(1) src=x>");
        assertFalse(result.toLowerCase().contains("onerror="));
    }

    @Test
    void xssSanitizer_NullAndEmpty_HandledCorrectly() {
        XssSanitizer sanitizer = new XssSanitizer();
        assertNull(sanitizer.sanitize(null));
        assertEquals("", sanitizer.sanitize(""));
    }

    @Test
    void xssSanitizer_SanitizeForJson_EscapesSpecialChars() {
        XssSanitizer sanitizer = new XssSanitizer();
        String result = sanitizer.sanitizeForJson("line1\nline2\t\"quoted\"");
        assertTrue(result.contains("\\n"));
        assertTrue(result.contains("\\t"));
        assertTrue(result.contains("\\\""));
    }

    @Test
    void xssSanitizer_SanitizeForJson_NullReturnsNull() {
        XssSanitizer sanitizer = new XssSanitizer();
        assertNull(sanitizer.sanitizeForJson(null));
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

    @Test
    void validationUtils_ValidatePageParams_Valid_DoesNotThrow() {
        assertDoesNotThrow(() ->
                ValidationUtils.validatePageParams(0, 50, 100));
    }

    @Test
    void validationUtils_ValidatePageParams_NegativePage_Throws() {
        assertThrows(ValidationException.class, () ->
                ValidationUtils.validatePageParams(-1, 50, 100));
    }

    @Test
    void validationUtils_ValidatePageParams_SizeTooLarge_Throws() {
        assertThrows(ValidationException.class, () ->
                ValidationUtils.validatePageParams(0, 200, 100));
    }
}
