package com.excelviewer.service;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextNormalizer {

    private static final Pattern LEADING_TAG = Pattern.compile("^([A-Za-z]{1,8}\\s+\\d[\\d_]*)");

    private TextNormalizer() {
    }

    public static String cleanTag(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\_", "_").trim().replaceAll("\\s+", " ");
    }

    public static String normalizeKey(String value) {
        return cleanTag(value).toUpperCase(Locale.ROOT);
    }

    public static String extractLeadingTag(String value) {
        String cleaned = cleanTag(value);
        if (cleaned.isEmpty()) {
            return "";
        }
        Matcher matcher = LEADING_TAG.matcher(cleaned);
        return matcher.find() ? cleanTag(matcher.group(1)) : "";
    }

    public static boolean looksLikeTagLabel(String value) {
        String key = normalizeKey(value);
        return key.equals("TAG")
                || key.equals("TAG NO")
                || key.equals("TAG NO.")
                || key.equals("TAG NUMBER")
                || key.equals("TAGNO")
                || key.equals("P&ID TAG")
                || key.equals("PID TAG")
                || key.startsWith("TAG ");
    }

    public static String cellText(String value) {
        return value == null ? "" : value.trim();
    }

    public static String htmlEscape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
