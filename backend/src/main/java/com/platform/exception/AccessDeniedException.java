// src/main/java/com/platform/exception/AccessDeniedException.java
package com.platform.exception;

import lombok.Getter;

@Getter
public class AccessDeniedException extends RuntimeException {

    private final String requiredRole;
    private final String userId;

    public AccessDeniedException(String message) {
        super(message);
        this.requiredRole = null;
        this.userId = null;
    }

    public AccessDeniedException(String message, String requiredRole) {
        super(message);
        this.requiredRole = requiredRole;
        this.userId = null;
    }

    public AccessDeniedException(String message, String userId, String requiredRole) {
        super(message);
        this.userId = userId;
        this.requiredRole = requiredRole;
    }

    public AccessDeniedException(String message, Throwable cause) {
        super(message, cause);
        this.requiredRole = null;
        this.userId = null;
    }
}