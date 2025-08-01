package com.jobmatcher.server.util;

public class SanitizationUtil {
    private static final String ANGLE_BRACKETS = "[<>]";
    private static final String QUOTES = "[\"']";

    private SanitizationUtil() {}

    public static String sanitizeText(String input) {
        if (input == null) return null;
        // Remove HTML/script injection vectors
        return input.replaceAll(ANGLE_BRACKETS, "").trim();
    }

    public static String sanitizeUrl(String input) {
        if (input == null || input.isBlank()) return null;
        // Remove HTML/script injection vectors and trim
        String sanitized = input.trim().replaceAll(ANGLE_BRACKETS, "").replaceAll(QUOTES, "");
        if (!isValidUrl(sanitized)) return null;
        return sanitized;
    }

    private static boolean isValidUrl(String url) {
        return url.matches("^(https?://)[\\w\\-]+(\\.[\\w\\-]+)+[/#?]?.*$") &&
                !url.contains("localhost") &&
                !url.matches(".*(\\b192\\.168\\.|\\b10\\.|\\b172\\.(1[6-9]|2\\d|3[01])).*");
    }
}
