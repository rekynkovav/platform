package com.platform.exception;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CustomExceptionsTest {

    // ==================== BusinessException ====================

    @Test
    void businessException_WithMessage_SetsMessage() {
        BusinessException ex = new BusinessException("Business rule violated");
        assertEquals("Business rule violated", ex.getMessage());
    }

    @Test
    void businessException_WithCause_SetsCauseAndMessage() {
        RuntimeException cause = new RuntimeException("root cause");
        BusinessException ex = new BusinessException("Wrapped error", cause);

        assertEquals("Wrapped error", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    // ==================== ResourceNotFoundException ====================

    @Test
    void resourceNotFoundException_SetsMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void resourceNotFoundException_IsRuntimeException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("test");
        assertInstanceOf(RuntimeException.class, ex);
    }

    // ==================== ValidationException ====================

    @Test
    void validationException_WithMessageOnly_HasEmptyErrorsMap() {
        ValidationException ex = new ValidationException("Validation failed");
        assertEquals("Validation failed", ex.getMessage());
        assertNotNull(ex.getValidationErrors());
        assertTrue(ex.getValidationErrors().isEmpty());
    }

    @Test
    void validationException_WithErrorsMap_SetsErrors() {
        Map<String, String> errors = Map.of("email", "invalid", "name", "required");
        ValidationException ex = new ValidationException("Validation failed", errors);

        assertEquals(2, ex.getValidationErrors().size());
        assertEquals("invalid", ex.getValidationErrors().get("email"));
    }

    @Test
    void validationException_WithNullErrorsMap_CreatesEmptyMap() {
        ValidationException ex = new ValidationException("msg", (Map<String, String>) null);
        assertNotNull(ex.getValidationErrors());
        assertTrue(ex.getValidationErrors().isEmpty());
    }

    @Test
    void validationException_WithFieldAndError_FormatsMessageAndSetsError() {
        ValidationException ex = new ValidationException("email", "must be valid");

        assertTrue(ex.getMessage().contains("email"));
        assertTrue(ex.getMessage().contains("must be valid"));
        assertEquals("must be valid", ex.getValidationErrors().get("email"));
    }

    @Test
    void validationException_AddError_AddsToMap() {
        ValidationException ex = new ValidationException("initial");
        ex.addError("phone", "too short");

        assertEquals("too short", ex.getValidationErrors().get("phone"));
    }
}
