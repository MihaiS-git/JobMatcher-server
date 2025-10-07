package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.MilestoneStatus;
import com.jobmatcher.server.domain.PaymentStatus;
import com.jobmatcher.server.domain.Priority;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class MilestoneResponseDTO {

    private UUID id;
    private String title;
    private String description;
    private BigDecimal amount;
    private BigDecimal penaltyAmount;
    private BigDecimal bonusAmount;
    private Integer estimatedDuration; // in days
    private MilestoneStatus status;
    private PaymentStatus paymentStatus;
    private String notes;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
    private Priority priority;
    private UUID contractId;
    private UUID invoiceId;
    private UUID paymentId;

}
