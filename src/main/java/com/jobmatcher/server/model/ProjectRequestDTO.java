package com.jobmatcher.server.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jobmatcher.server.domain.PaymentType;
import com.jobmatcher.server.domain.ProjectStatus;
import com.jobmatcher.server.validator.CreateUpdateValidation;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
@Jacksonized
@ToString
public class ProjectRequestDTO {
    @NotNull(groups = CreateUpdateValidation.OnCreate.class)
    private UUID customerId;

    private UUID freelancerId;

    private UUID contractId;

    @Size(max=255, message = "Title must be up to 255 characters")
    @NotBlank(groups = CreateUpdateValidation.OnCreate.class, message = "Title cannot be blank")
    private String title;

    @Size(max=2000, message = "Description must be up to 2000 characters")
    @NotBlank(groups = CreateUpdateValidation.OnCreate.class, message = "Description cannot be blank")
    private String description;

    @DecimalMin("0.0")
    @NotNull(groups = CreateUpdateValidation.OnCreate.class, message = "Budget must be a positive number")
    private BigDecimal budget;

    @NotNull(groups = CreateUpdateValidation.OnCreate.class, message = "Payment type must be provided")
    private PaymentType paymentType;

    @Future(message = "Deadline must be a future date")
    @NotNull(groups = CreateUpdateValidation.OnCreate.class, message = "Deadline must be a future date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate deadline;

    @NotNull(groups = CreateUpdateValidation.OnCreate.class, message = "Category ID must be provided")
    private Long categoryId;

    @Size(max = 5, message = "You can select up to 5 subcategories")
    @NotNull(groups = CreateUpdateValidation.OnCreate.class, message = "At least one subcategory must be selected")
    private Set<Long> subcategoryIds;

    private UUID acceptedProposalId;
}
