package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.ContractStatus;

import com.jobmatcher.server.domain.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class ContractRequestDTO {
    private ContractStatus status;
    private UUID invoiceId;
    private UUID paymentId;
    private BigDecimal totalPaid;
    private PaymentStatus paymentStatus;
    private OffsetDateTime completedAt;
    private OffsetDateTime terminatedAt;
}
