// src/main/java/com/platform/exception/BadRequestException.java
package com.platform.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class BadRequestException extends RuntimeException {

    private final Map<String, String> errors;

    public BadRequestException(String message) {
        super(message);
        this.errors = null;
    }

    public BadRequestException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
        this.errors = null;
    }
}