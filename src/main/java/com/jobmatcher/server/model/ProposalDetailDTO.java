package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.PaymentStatus;
import com.jobmatcher.server.domain.Priority;
import com.jobmatcher.server.domain.ProposalStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class ProposalDetailDTO {

    private UUID id;
    private UUID projectId;
    private FreelancerSummaryDTO freelancer;
    private String coverLetter;
    private BigDecimal amount;
    private BigDecimal penaltyAmount;
    private BigDecimal bonusAmount;
    private Integer estimatedDuration; // in days
    private ProposalStatus status;
    private String notes;
    private String plannedStartDate;
    private String plannedEndDate;
    private String actualStartDate;
    private String actualEndDate;
    private Priority priority;
    private Set<UUID> milestonesIds;
    private UUID contractId;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastUpdate;
}
