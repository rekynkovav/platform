package com.platform.util;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
public class SqlValidator {

    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(\\b(select|insert|update|delete|drop|alter|create|truncate|union|exec|execute|declare|merge|replace)\\b|(--|;|'|\"|\\*/|/\\*))"
    );

    private static final Pattern SQL_KEYWORDS_WITH_QUOTES = Pattern.compile(
        "(?i)(\\b(or|and)\\s+\\w+\\s*(=|<>|<|>|like|in)\\s*'.*')"
    );

    private static final Pattern SQL_OR_AND_INJECTION = Pattern.compile(
        "(?i)\\b(or|and)\\s+\\d+\\s*=\\s*\\d+"
    );

    public boolean isSafe(String input) {
        if (input == null || input.isEmpty()) return true;

        // Проверка на SQL инъекции (ключевые слова, спецсимволы)
        if (SQL_INJECTION_PATTERN.matcher(input).find()) {
            return false;
        }

        // Проверка на OR/AND инъекции с кавычками
        if (SQL_KEYWORDS_WITH_QUOTES.matcher(input).find()) {
            return false;
        }

        // Проверка на простые OR/AND инъекции типа "1 OR 1=1"
        if (SQL_OR_AND_INJECTION.matcher(input).find()) {
            return false;
        }

        return true;
    }

    public String sanitize(String input) {
        if (input == null) return null;
        if (input.isEmpty()) return input;

        // Удаление опасных символов
        String sanitized = input
            .replace("'", "")
            .replace("\"", "")
            .replace(";", "")
            .replace("--", "")
            .replace("/*", "")
            .replace("*/", "")
            .replace("\\", "")
            .replace("%", "")
            .replace("_", "");

        return sanitized.trim();
    }

    public boolean isValidOrderBy(String input) {
        if (input == null || input.isEmpty()) return true;
        return Pattern.matches("^[a-zA-Z_][a-zA-Z0-9_]*$", input);
    }
}
