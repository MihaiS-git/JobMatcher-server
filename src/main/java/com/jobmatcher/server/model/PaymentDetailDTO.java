package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class PaymentDetailDTO {
    private UUID id;
    private ContractSummaryDTO contract;
    private MilestoneResponseDTO milestone;
    private InvoiceSummaryDTO invoice;
    private BigDecimal amount;
    private PaymentStatus status;
    private OffsetDateTime paidAt;
    private String notes;
}
