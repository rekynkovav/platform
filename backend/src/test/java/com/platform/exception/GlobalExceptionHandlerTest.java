package com.platform.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ==================== ResourceNotFoundException → 404 ====================

    @Test
    void handleNotFound_Returns404WithMessage() {
        ResponseEntity<Map<String, String>> response = handler.handleNotFound(
                new ResourceNotFoundException("User not found"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User not found", response.getBody().get("error"));
    }

    // ==================== AccessDeniedException → 403 ====================

    @Test
    void handleAccessDenied_Returns403WithMessage() {
        ResponseEntity<Map<String, String>> response = handler.handleAccessDenied(
                new AccessDeniedException("Forbidden"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Forbidden", response.getBody().get("error"));
    }

    // ==================== BusinessException → 400 ====================

    @Test
    void handleBusiness_Returns400WithMessage() {
        ResponseEntity<Map<String, String>> response = handler.handleBusiness(
                new BusinessException("Invalid operation"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid operation", response.getBody().get("error"));
    }

    // ==================== MethodArgumentNotValidException → 400 ====================

    @Test
    void handleValidation_Returns400WithFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("dto", "email", "must be valid");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation failed", response.getBody().get("error"));
        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) response.getBody().get("details");
        assertEquals("must be valid", details.get("email"));
    }

    // ==================== ConstraintViolationException → 400 ====================

    @Test
    void handleConstraintViolation_Returns400WithMessages() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("must not be blank");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));
        ResponseEntity<Map<String, Object>> response = handler.handleConstraintViolation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Constraint violation", response.getBody().get("error"));
        assertTrue(response.getBody().get("details").toString().contains("must not be blank"));
    }

    // ==================== HttpMessageNotReadableException → 400 ====================

    @Test
    void handleInvalidJson_Returns400() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("bad json");
        ResponseEntity<Map<String, String>> response = handler.handleInvalidJson(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid request body format", response.getBody().get("error"));
    }

    // ==================== MethodArgumentTypeMismatchException → 400 ====================

    @Test
    void handleTypeMismatch_Returns400WithParamInfo() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "page", null, null);
        ResponseEntity<Map<String, String>> response = handler.handleTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().get("error").contains("page"));
    }

    // ==================== OptimisticLockingFailureException → 409 ====================

    @Test
    void handleOptimisticLock_Returns409() {
        ResponseEntity<Map<String, String>> response = handler.handleOptimisticLock(
                new OptimisticLockingFailureException("concurrent update"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody().get("error").contains("retry"));
    }

    // ==================== Generic Exception → 500 ====================

    @Test
    void handleGeneric_Returns500() {
        ResponseEntity<Map<String, String>> response = handler.handleGeneric(
                new RuntimeException("unexpected"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Internal server error", response.getBody().get("error"));
    }
}
