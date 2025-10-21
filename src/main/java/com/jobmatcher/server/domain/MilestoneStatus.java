package com.jobmatcher.server.domain;

public enum MilestoneStatus {
    PENDING,    // Not started yet
    IN_PROGRESS,    // Work ongoing
    COMPLETED,   // Accepted by client
    PAID,       // Payment processed (if per-milestone)
    CANCELLED;
}
