package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDetailDTO {
    private UUID id;
    private CustomerSummaryDTO customer;
    private FreelancerSummaryDTO freelancer;
    private String title;
    private String description;
    private ProjectStatus status;
    private BigDecimal budget;
    private PaymentType paymentType;
    private LocalDate deadline;
    private JobCategoryDTO category;
    private Set<JobSubcategoryDTO> subcategories;
    private Set<ProposalSummaryDTO> proposals;
    private UUID acceptedProposalId;
    private UUID contractId;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastUpdate;
}
