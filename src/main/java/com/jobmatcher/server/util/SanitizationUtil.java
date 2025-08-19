package com.jobmatcher.server.util;

import org.apache.commons.validator.routines.UrlValidator;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import java.text.Normalizer;
import java.util.Set;
import java.util.stream.Collectors;

public class SanitizationUtil {

    private static final PolicyFactory HTML_POLICY = new HtmlPolicyBuilder()
            .allowElements("b", "i", "strong", "em", "u", "p", "ul", "ol", "li", "br")
            .allowUrlProtocols("http", "https")
            .toFactory();

    private static final UrlValidator URL_VALIDATOR = new UrlValidator(new String[]{"http", "https"});

    private static final int MAX_TEXT_LENGTH = 2000;

    public static String sanitizeUrl(String url) {
        if (url == null || url.isBlank()) return null;

        String trimmed = Normalizer.normalize(url.trim(), Normalizer.Form.NFKC);

        if (!URL_VALIDATOR.isValid(trimmed)) return null;

        // Store clean raw value, don't encode yet
        return trimmed;
    }

    public static String sanitizeText(String text) {
        if (text == null || text.isBlank()) return null;

        // Normalize unicode (avoids invisible homoglyph tricks)
        String normalized = Normalizer.normalize(text.trim(), Normalizer.Form.NFKC);

        // Enforce length cap (business rule – 2000 chars for descriptions)
        if (normalized.length() > 2000) {
            normalized = normalized.substring(0, 2000);
        }

        // Apply strict HTML policy (strip or allow only safe inline tags)
        return HTML_POLICY.sanitize(normalized);
    }

    public static Set<String> sanitizeUrls(Set<String> urls) {
        if (urls == null) return Set.of();

        return urls.stream()
                .map(SanitizationUtil::sanitizeUrl)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toSet());
    }
}
