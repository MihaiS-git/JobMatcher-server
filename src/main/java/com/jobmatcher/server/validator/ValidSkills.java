package com.jobmatcher.server.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = SkillsValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSkills {
    String message() default "Skills contain invalid entries.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
