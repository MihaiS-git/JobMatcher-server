package com.jobmatcher.server.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ProjectStatus {
    DRAFT,        // Created but not published
    OPEN,         // Accepting proposals
    IN_PROGRESS,  // A contract started
    COMPLETED,    // Work done and accepted
    STOPPED;     // No longer active

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