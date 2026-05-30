package com.platform.util;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
public class SqlValidator {

    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(\\b(select|insert|update|delete|drop|alter|create|truncate|union|exec|execute|declare|merge|replace)\\b|(--|;|'|\"|\\*/|/\\*))"
    );

    private static final Pattern SQL_KEYWORDS = Pattern.compile(
        "(?i)(\\b(or|and)\\s+\\w+\\s*(=|<>|<|>|like|in)\\s*'.*')"
    );

    private static final Pattern SINGLE_QUOTE_PATTERN = Pattern.compile("('.*?')");

    public boolean isSafe(String input) {
        if (input == null || input.isEmpty()) return true;

        // Проверка на SQL инъекции
        if (SQL_INJECTION_PATTERN.matcher(input).find()) {
            return false;
        }

        // Проверка на OR/AND инъекции
        if (SQL_KEYWORDS.matcher(input).find()) {
            return false;
        }

        return true;
    }

    public String sanitize(String input) {
        if (input == null) return null;
        if (input.isEmpty()) return input;

        // Экранирование спецсимволов
        String sanitized = input
            .replace("'", "''")
            .replace(";", "")
            .replace("--", "")
            .replace("/*", "")
            .replace("*/", "")
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");

        return sanitized.trim();
    }

    public boolean isValidOrderBy(String input) {
        if (input == null || input.isEmpty()) return true;
        return Pattern.matches("^[a-zA-Z_][a-zA-Z0-9_]*$", input);
    }
}