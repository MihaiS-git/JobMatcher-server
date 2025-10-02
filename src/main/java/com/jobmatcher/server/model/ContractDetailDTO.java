package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.*;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class ContractDetailDTO {
    private UUID id;
    private UUID proposalId;
    private UUID projectId;
    private UUID customerId;
    private UUID freelancerId;
    private String customerName;
    private String freelancerName;
    private ContactDTO customerContact;
    private ContactDTO freelancerContact;
    private ContractStatus status;
    private String title;
    private String description;
    private BigDecimal amount;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private PaymentType paymentType;
    private Set<MilestoneResponseDTO> milestones;
    private UUID invoiceId;
    private UUID paymentId;
    private BigDecimal totalPaid;
    private BigDecimal remainingBalance;
    private PaymentStatus paymentStatus;
    private OffsetDateTime signedAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime terminatedAt;
}
