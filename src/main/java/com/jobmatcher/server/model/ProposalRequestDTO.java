package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.PaymentStatus;
import com.jobmatcher.server.domain.Priority;
import com.jobmatcher.server.domain.ProposalStatus;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class ProposalRequestDTO {

    @NotNull
    private UUID projectId;

    @NotNull
    private UUID freelancerId;

    @Size(max=2000, message = "Cover letter cannot exceed 2000 characters.")
    private String coverLetter;

    @DecimalMin(value = "0.0", inclusive = false, message = "Proposed amount must be greater than zero.")
    @Digits(integer = 12, fraction = 2, message = "Proposed amount must be a valid monetary value.")
    private BigDecimal amount;

    @DecimalMin(value = "0.0", message = "Amount must be greater than zero.")
    private BigDecimal penaltyAmount;

    @DecimalMin(value = "0.0", message = "Amount must be greater than zero.")
    private BigDecimal bonusAmount;

    @Min(value = 1, message = "Estimated duration must be at least 1 day.")
    private Integer estimatedDuration; // in days

    private ProposalStatus status;

    @Size(max=2000, message = "Notes cannot exceed 2000 characters.")
    private String notes;

    private OffsetDateTime plannedStartDate;
    private OffsetDateTime plannedEndDate;
    private OffsetDateTime actualStartDate;
    private OffsetDateTime actualEndDate;

    private Priority priority;

}
