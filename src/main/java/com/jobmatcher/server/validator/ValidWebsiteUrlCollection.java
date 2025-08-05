package com.jobmatcher.server.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidWebsiteUrlCollectionValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidWebsiteUrlCollection {
    String message() default "One or more URLs are invalid.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}