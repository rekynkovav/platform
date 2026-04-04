// src/main/java/com/platform/exception/ValidationException.java
package com.platform.exception;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ValidationException extends RuntimeException {

    private final Map<String, String> validationErrors;

    public ValidationException(String message) {
        super(message);
        this.validationErrors = new HashMap<>();
    }

    public ValidationException(String message, Map<String, String> validationErrors) {
        super(message);
        this.validationErrors = validationErrors != null ? validationErrors : new HashMap<>();
    }

    public ValidationException(String field, String error) {
        super(String.format("Validation failed for field '%s': %s", field, error));
        this.validationErrors = new HashMap<>();
        this.validationErrors.put(field, error);
    }

    public void addError(String field, String error) {
        this.validationErrors.put(field, error);
    }
}