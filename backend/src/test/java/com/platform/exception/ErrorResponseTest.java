package com.platform.exception;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    @Test
    void of_WithFourParams_SetsAllFields() {
        ErrorResponse response = ErrorResponse.of(404, "Not Found", "Resource missing", "/api/test");

        assertEquals(404, response.getStatus());
        assertEquals("Not Found", response.getError());
        assertEquals("Resource missing", response.getMessage());
        assertEquals("/api/test", response.getPath());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void of_WithErrorCode_SetsErrorCodeField() {
        ErrorResponse response = ErrorResponse.of(400, "Bad Request", "Invalid input", "/api/data", "VALIDATION_ERROR");

        assertEquals(400, response.getStatus());
        assertEquals("VALIDATION_ERROR", response.getErrorCode());
        assertEquals("Invalid input", response.getMessage());
    }

    @Test
    void of_WithValidationErrors_SetsValidationErrorsMap() {
        Map<String, String> errors = Map.of("email", "must be valid", "name", "required");
        ErrorResponse response = ErrorResponse.of(422, "Validation Failed", errors, "/api/register");

        assertEquals(422, response.getStatus());
        assertNotNull(response.getValidationErrors());
        assertEquals(2, response.getValidationErrors().size());
        assertEquals("must be valid", response.getValidationErrors().get("email"));
    }

    @Test
    void builder_AllowsSettingTraceId() {
        ErrorResponse response = ErrorResponse.builder()
                .status(500)
                .error("Internal Server Error")
                .message("Something went wrong")
                .path("/api/crash")
                .traceId("abc-123-trace")
                .timestamp(LocalDateTime.now())
                .build();

        assertEquals("abc-123-trace", response.getTraceId());
    }

    @Test
    void timestamp_IsSetByFactoryMethods() {
        ErrorResponse response = ErrorResponse.of(500, "Error", "msg", "/path");
        assertNotNull(response.getTimestamp());
    }
}
