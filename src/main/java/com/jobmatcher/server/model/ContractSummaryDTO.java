package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.ContractStatus;
import com.jobmatcher.server.domain.PaymentType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class ContractSummaryDTO {
    private UUID id;
    private String customerName;
    private String freelancerName;
    private ContractStatus status;
    private String title;
    private BigDecimal amount;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private PaymentType paymentType;
}
