package com.jobmatcher.server.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ProjectStatus {
    OPEN,
    PROPOSALS_RECEIVED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    NONE;

    @JsonCreator
    public static ProjectStatus fromString(String value) {
        if (value == null) return null;
        try {
            return ProjectStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid ProjectStatus: " + value);
        }
    }
}
