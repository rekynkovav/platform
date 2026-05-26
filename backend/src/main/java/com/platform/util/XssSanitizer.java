package com.platform.util;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
public class XssSanitizer {

    private static final Pattern[] XSS_PATTERNS = {
        Pattern.compile("<script(.*?)</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onload=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onerror=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onclick=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onmouseover=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onmouseout=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onfocus=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onblur=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onchange=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onsubmit=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onreset=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onselect=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onkeydown=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onkeypress=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onkeyup=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("&#", Pattern.CASE_INSENSITIVE)
    };

    private static final Pattern ALLOWED_HTML = Pattern.compile(
        "<(b|strong|i|em|u|p|br|a href=\"[^\"]*\")[^>]*>",
        Pattern.CASE_INSENSITIVE
    );

    public String sanitize(String input) {
        if (input == null) return null;
        if (input.isEmpty()) return "";

        String clean = input;

        // Удаление опасных паттернов
        for (Pattern pattern : XSS_PATTERNS) {
            clean = pattern.matcher(clean).replaceAll("");
        }

        // Экранирование HTML
        clean = clean
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;")
            .replace("\\", "&#x5C;");

        return clean;
    }

    public String sanitizeForHtml(String input) {
        if (input == null) return null;
        return sanitize(input);
    }

    public String sanitizeForJson(String input) {
        if (input == null) return null;
        return input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("/", "\\/")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}