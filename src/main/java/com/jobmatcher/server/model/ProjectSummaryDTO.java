package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.PaymentType;
import com.jobmatcher.server.domain.ProjectStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSummaryDTO {
    private UUID id;
    private UUID customerId;
    private UUID freelancerId;
    private String title;
    private String description;
    private ProjectStatus status;
    private BigDecimal budget;
    private PaymentType paymentType;
    private LocalDate deadline;
    private JobCategoryDTO category;
    private Set<JobSubcategoryDTO> subcategories;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastUpdate;
}
