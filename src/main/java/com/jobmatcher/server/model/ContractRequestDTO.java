package com.jobmatcher.server.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class ContractRequestDTO {
    private UUID invoiceId;
    private UUID paymentId;
    private BigDecimal totalPaid;
    private OffsetDateTime completedAt;
    private OffsetDateTime terminatedAt;
}
