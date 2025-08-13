package com.jobmatcher.server.util;

import org.apache.commons.validator.routines.UrlValidator;
import org.owasp.encoder.Encode;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import java.util.Set;
import java.util.stream.Collectors;

public class SanitizationUtil {

    private static final PolicyFactory HTML_POLICY = new HtmlPolicyBuilder()
            .allowElements("b", "i", "strong", "em", "u", "p", "ul", "ol", "li", "br")
            .allowUrlProtocols("http", "https")
            .toFactory();

    private static final UrlValidator URL_VALIDATOR = new UrlValidator(new String[]{"http", "https"});

    public static String sanitizeUrl(String url) {
        if (url == null || url.isBlank()) return null;

        String trimmed = url.trim();
        if (!URL_VALIDATOR.isValid(trimmed)) return null;

        return Encode.forHtml(trimmed); // encode special chars for safe HTML
    }

    public static String sanitizeText(String text) {
        if (text == null || text.isBlank()) return null;
        return HTML_POLICY.sanitize(text);
    }

    public static Set<String> sanitizeUrls(Set<String> urls) {
        if (urls == null) return Set.of();

        return urls.stream()
                .map(SanitizationUtil::sanitizeUrl)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toSet());
    }
}
