package com.jobmatcher.server.domain;

public enum PaymentStatus {
    PAID,            // Fully paid
    REFUNDED,        // Refunded
    FAILED;          // Failed transaction
}
