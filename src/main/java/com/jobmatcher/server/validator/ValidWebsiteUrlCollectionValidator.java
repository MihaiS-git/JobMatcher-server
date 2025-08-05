package com.jobmatcher.server.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class ValidWebsiteUrlCollectionValidator implements ConstraintValidator<ValidWebsiteUrlCollection, Set<String>> {

    private final WebsiteUrlValidator urlValidator = new WebsiteUrlValidator();

    @Override
    public void initialize(ValidWebsiteUrlCollection constraintAnnotation) {
        urlValidator.initialize(null); // if needed, otherwise remove
    }

    @Override
    public boolean isValid(Set<String> urls, ConstraintValidatorContext context) {
        if (urls == null || urls.isEmpty()) {
            return true; // null or empty collections are valid
        }
        for (String url : urls) {
            if (url == null || !urlValidator.isValid(url, context)) {
                return false;
            }
        }
        return true;
    }
}
