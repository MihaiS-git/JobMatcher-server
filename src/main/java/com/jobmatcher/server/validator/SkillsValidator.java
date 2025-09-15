package com.jobmatcher.server.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;
import java.util.regex.Pattern;

public class SkillsValidator implements ConstraintValidator<ValidSkills, Set<String>> {

    private static final Pattern SKILL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9șȘțȚăĂâÂîÎéè ./#_\\-+()&*@:%]{1,50}$",
            Pattern.UNICODE_CHARACTER_CLASS
    );

    @Override
    public boolean isValid(Set<String> skills, ConstraintValidatorContext context) {
        if (skills == null || skills.isEmpty()) return true;

        for (String skill : skills) {
            if (skill == null) return false;

            String trimmed = skill.trim().replaceAll("\\p{C}", "");
            if (trimmed.isEmpty()) return false;

            if (!SKILL_PATTERN.matcher(trimmed).matches()) return false;
        }

        return true;
    }
}
