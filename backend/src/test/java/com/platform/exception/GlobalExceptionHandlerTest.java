package com.platform.exception;

import jakarta.servlet.http.HttpServletRequest;
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
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    // ==================== ResourceNotFoundException → 404 ====================

    @Test
    void handleNotFound_Returns404WithMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(
                new ResourceNotFoundException("User not found"), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User not found", response.getBody().getMessage());
        assertEquals(404, response.getBody().getStatus());
    }

    // ==================== AccessDeniedException → 403 ====================

    @Test
    void handleAccessDenied_Returns403WithMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(
                new AccessDeniedException("Forbidden"), request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Forbidden", response.getBody().getMessage());
        assertEquals(403, response.getBody().getStatus());
    }

    // ==================== BusinessException → 400 ====================

    @Test
    void handleBusiness_Returns400WithMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleBusiness(
                new BusinessException("Invalid operation"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid operation", response.getBody().getMessage());
        assertEquals(400, response.getBody().getStatus());
    }

    // ==================== MethodArgumentNotValidException → 400 ====================

    @Test
    void handleValidation_Returns400WithFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("dto", "email", "must be valid");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation Failed", response.getBody().getError());
        assertEquals("must be valid", response.getBody().getValidationErrors().get("email"));
    }

    // ==================== ConstraintViolationException → 400 ====================

    @Test
    void handleConstraintViolation_Returns400WithMessages() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("must not be blank");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));
        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Constraint Violation", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("must not be blank"));
    }

    // ==================== HttpMessageNotReadableException → 400 ====================

    @Test
    void handleInvalidJson_Returns400() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("bad json");
        ResponseEntity<ErrorResponse> response = handler.handleInvalidJson(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid request body format", response.getBody().getMessage());
    }

    // ==================== MethodArgumentTypeMismatchException → 400 ====================

    @Test
    void handleTypeMismatch_Returns400WithParamInfo() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "page", null, null);
        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("page"));
    }

    // ==================== OptimisticLockingFailureException → 409 ====================

    @Test
    void handleOptimisticLock_Returns409() {
        ResponseEntity<ErrorResponse> response = handler.handleOptimisticLock(
                new OptimisticLockingFailureException("concurrent update"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("retry"));
    }

    // ==================== Generic Exception → 500 ====================

    @Test
    void handleGeneric_Returns500() {
        ResponseEntity<ErrorResponse> response = handler.handleGeneric(
                new RuntimeException("unexpected"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Internal server error", response.getBody().getMessage());
    }
}
