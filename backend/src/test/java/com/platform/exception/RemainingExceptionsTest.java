package com.platform.exception;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RemainingExceptionsTest {

    // ==================== AccessDeniedException ====================

    @Test
    void accessDeniedException_SetsMessage() {
        AccessDeniedException ex = new AccessDeniedException("No access");
        assertEquals("No access", ex.getMessage());
    }

    @Test
    void accessDeniedException_IsRuntimeException() {
        assertInstanceOf(RuntimeException.class, new AccessDeniedException("test"));
    }

    // ==================== BadRequestException ====================

    @Test
    void badRequestException_WithMessage_HasNullErrors() {
        BadRequestException ex = new BadRequestException("Bad input");
        assertEquals("Bad input", ex.getMessage());
        assertNull(ex.getErrors());
    }

    @Test
    void badRequestException_WithErrorsMap_SetsErrors() {
        Map<String, String> errors = Map.of("field", "invalid");
        BadRequestException ex = new BadRequestException("Validation error", errors);

        assertNotNull(ex.getErrors());
        assertEquals("invalid", ex.getErrors().get("field"));
    }

    @Test
    void badRequestException_WithCause_SetsCauseAndNullErrors() {
        RuntimeException cause = new RuntimeException("root");
        BadRequestException ex = new BadRequestException("wrapped", cause);

        assertEquals("wrapped", ex.getMessage());
        assertSame(cause, ex.getCause());
        assertNull(ex.getErrors());
    }

    // ==================== ConflictException ====================

    @Test
    void conflictException_WithMessageOnly_HasNullFields() {
        ConflictException ex = new ConflictException("Conflict occurred");
        assertEquals("Conflict occurred", ex.getMessage());
        assertNull(ex.getConflictingField());
        assertNull(ex.getConflictingValue());
    }

    @Test
    void conflictException_WithFieldAndValue_FormatsMessageAndSetsFields() {
        ConflictException ex = new ConflictException("Duplicate", "email", "test@test.com");

        assertTrue(ex.getMessage().contains("email"));
        assertTrue(ex.getMessage().contains("test@test.com"));
        assertEquals("email", ex.getConflictingField());
        assertEquals("test@test.com", ex.getConflictingValue());
    }

    @Test
    void conflictException_WithCause_SetsCauseAndNullFields() {
        RuntimeException cause = new RuntimeException("db error");
        ConflictException ex = new ConflictException("DB conflict", cause);

        assertSame(cause, ex.getCause());
        assertNull(ex.getConflictingField());
    }

    // ==================== UnauthorizedException ====================

    @Test
    void unauthorizedException_WithMessage_DefaultErrorCode() {
        UnauthorizedException ex = new UnauthorizedException("Not authenticated");
        assertEquals("Not authenticated", ex.getMessage());
        assertEquals("UNAUTHORIZED", ex.getErrorCode());
    }

    @Test
    void unauthorizedException_WithCustomErrorCode_SetsErrorCode() {
        UnauthorizedException ex = new UnauthorizedException("Token expired", "TOKEN_EXPIRED");
        assertEquals("TOKEN_EXPIRED", ex.getErrorCode());
    }

    @Test
    void unauthorizedException_WithCause_DefaultErrorCode() {
        RuntimeException cause = new RuntimeException("auth fail");
        UnauthorizedException ex = new UnauthorizedException("Auth failed", cause);

        assertSame(cause, ex.getCause());
        assertEquals("UNAUTHORIZED", ex.getErrorCode());
    }
}
