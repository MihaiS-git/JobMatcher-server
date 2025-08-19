package com.jobmatcher.server.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PaymentType {
    UPFRONT,
    MILESTONE,
    UPON_COMPLETION,
    COMMISSION,
    NONE;

    @JsonCreator
    public static PaymentType fromString(String value) {
        if (value == null) return null;
        try {
            return PaymentType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid PaymentType: " + value);
        }
    }
}
