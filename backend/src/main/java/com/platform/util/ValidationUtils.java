package com.platform.util;

import com.platform.exception.ValidationException;
import java.util.UUID;

public class ValidationUtils {

    public static void requireNonNull(Object obj, String message) {
        if (obj == null) throw new ValidationException(message);
    }

    public static void requireNotBlank(String str, String message) {
        if (str == null || str.isBlank()) throw new ValidationException(message);
    }

    public static void validateId(String id, String fieldName) {
        requireNotBlank(id, fieldName + " is required");
        try {
            UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(fieldName + " has invalid format");
        }
    }

    public static void validatePageParams(int page, int size, int maxSize) {
        if (page < 0) throw new ValidationException("Page cannot be negative");
        if (size < 1 || size > maxSize) throw new ValidationException("Size must be between 1 and " + maxSize);
    }
}