package com.platform.util;

import java.util.regex.Pattern;

public final class SqlValidator {

    private static final Pattern SAFE_IDENTIFIER =
        Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private SqlValidator() {}

    public static boolean isValidIdentifier(String input) {
        if (input == null || input.isEmpty()) return true;
        return SAFE_IDENTIFIER.matcher(input).matches();
    }
}
