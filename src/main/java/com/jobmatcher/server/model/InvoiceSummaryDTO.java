package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.InvoiceStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class InvoiceSummaryDTO {

    private UUID id;
    private ContractSummaryDTO contract;
    private MilestoneResponseDTO milestone;
    private BigDecimal amount;
    private OffsetDateTime issuedAt;
    private OffsetDateTime dueDate;
    private InvoiceStatus status;

}
