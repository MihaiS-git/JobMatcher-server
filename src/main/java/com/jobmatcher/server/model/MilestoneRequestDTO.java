package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class MilestoneRequestDTO {

    @NotNull
    private UUID contractId;

    @Size(max=255, message = "Title cannot exceed 255 characters.")
    private String title;

    @Size(max=2000, message = "Description cannot exceed 2000 characters.")
    private String description;

    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than zero.")
    private BigDecimal amount;

    @DecimalMin(value = "0.0", message = "Amount must be greater than zero.")
    private BigDecimal penaltyAmount;

    @DecimalMin(value = "0.0", message = "Amount must be greater than zero.")
    private BigDecimal bonusAmount;

    @Min(value = 1, message = "Estimated duration must be at least 1 day.")
    @Column(name = "estimated_duration")
    private Integer estimatedDuration; // in days

    private MilestoneStatus status;

    @Size(max=2000, message = "Notes cannot exceed 2000 characters.")
    private String notes;

    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
    private Priority priority;
}
