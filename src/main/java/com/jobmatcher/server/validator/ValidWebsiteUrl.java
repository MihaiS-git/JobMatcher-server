package com.jobmatcher.server.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = WebsiteUrlValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidWebsiteUrl {
    String message() default "Invalid website URL.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

