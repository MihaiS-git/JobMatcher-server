package com.jobmatcher.server.domain;

public enum ContractStatus {
    ACTIVE,      // Work ongoing
    ON_HOLD,     // Temporarily paused
    COMPLETED,   // All milestones done + accepted
    CANCELLED,   // Mutually cancelled before completion
    TERMINATED;  // Ended due to issues
}