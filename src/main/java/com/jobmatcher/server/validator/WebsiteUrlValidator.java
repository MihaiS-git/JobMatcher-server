package com.jobmatcher.server.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

public class WebsiteUrlValidator implements ConstraintValidator<ValidWebsiteUrl, String> {

    // Matches domains like example.com, my-site.org, abc.co.uk
    private static final Pattern TLD_PATTERN = Pattern.compile("^[^\\s]+\\.(com|org|net|io|co|ai|dev|app|info|biz|edu|gov|us|uk|de|fr|it|es|ca|au|nl|se|ch|be|jp|kr|in|br|za)(\\.[a-z]{2})?$", Pattern.CASE_INSENSITIVE);

    // Regex for private/local IPs and localhost
    private static final Pattern LOCALHOST_OR_PRIVATE_IP_PATTERN = Pattern.compile(
            "^(localhost|127\\.\\d+\\.\\d+\\.\\d+|10\\.\\d+\\.\\d+\\.\\d+|192\\.168\\.\\d+\\.\\d+|172\\.(1[6-9]|2\\d|3[0-1])\\.\\d+\\.\\d+)$",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }

        try {
            URI uri = new URI(value.trim());

            // Scheme must be http or https
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                return false;
            }

            // Host must exist and be a valid domain or IP
            String host = uri.getHost();
            if (host == null || host.length() > 253) {
                return false;
            }

            // Reject localhost and private IPs
            if (LOCALHOST_OR_PRIVATE_IP_PATTERN.matcher(host).matches()) {
                return false;
            }

            // Require a known TLD
            if (!TLD_PATTERN.matcher(host).matches()) {
                return false;
            }

            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}

