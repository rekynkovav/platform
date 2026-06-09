package com.platform.util;

import org.springframework.stereotype.Component;

@Component
public class XssSanitizer {

    public String sanitize(String input) {
        if (input == null) return null;
        if (input.isEmpty()) return "";

        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;");
    }

    public String sanitizeForHtml(String input) {
        return sanitize(input);
    }
}
