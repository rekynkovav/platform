// src/main/java/com/platform/exception/ConflictException.java
package com.platform.exception;

import lombok.Getter;

@Getter
public class ConflictException extends RuntimeException {

    private final String conflictingField;
    private final Object conflictingValue;

    public ConflictException(String message) {
        super(message);
        this.conflictingField = null;
        this.conflictingValue = null;
    }

    public ConflictException(String message, String conflictingField, Object conflictingValue) {
        super(String.format("%s: %s already exists with value: '%s'", message, conflictingField, conflictingValue));
        this.conflictingField = conflictingField;
        this.conflictingValue = conflictingValue;
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
        this.conflictingField = null;
        this.conflictingValue = null;
    }
}