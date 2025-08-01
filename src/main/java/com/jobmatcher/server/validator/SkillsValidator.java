package com.jobmatcher.server.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;
import java.util.regex.Pattern;

public class SkillsValidator implements ConstraintValidator<ValidSkills, Set<String>> {

    // Match letters, numbers, Romanian letters, and allowed symbols
    private static final Pattern SKILL_PATTERN = Pattern.compile("^[A-Za-z0-9șȘțȚăĂâÂîÎéè.\\-+# ]+$", Pattern.UNICODE_CHARACTER_CLASS);

    @Override
    public boolean isValid(Set<String> skills, ConstraintValidatorContext context) {
        if (skills == null || skills.isEmpty()) {
            return true; // Optional field; use @NotEmpty if required
        }

        for (String skill : skills) {
            if (skill == null || skill.trim().isEmpty()) {
                return false;
            }

            String trimmed = skill.trim();

            if (trimmed.length() < 1) {
                return false;
            }

            if (!SKILL_PATTERN.matcher(trimmed).matches()) {
                return false;
            }
        }

        return true;
    }
}
