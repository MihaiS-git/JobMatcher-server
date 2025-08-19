package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.*;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class ProjectResponseDTO {
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
}
