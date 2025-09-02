package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.PaymentType;
import com.jobmatcher.server.domain.ProjectStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class ProjectSummaryResponseDTO {
    private UUID id;
    private String title;
    private String description;
    private ProjectStatus status;
    private BigDecimal budget;
    private PaymentType paymentType;
    private LocalDate deadline;
    private JobCategoryDTO category;
    private Set<JobSubcategoryDTO> subcategories;
}
