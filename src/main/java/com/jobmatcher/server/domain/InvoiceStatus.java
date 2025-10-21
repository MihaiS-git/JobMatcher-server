package com.jobmatcher.server.domain;

public enum InvoiceStatus {
    PENDING,     // Awaiting payment
    PAID,        // Fully paid
    CANCELLED;   // Invalidated
}
