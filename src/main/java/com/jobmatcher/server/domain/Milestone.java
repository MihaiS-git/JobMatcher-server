package com.jobmatcher.server.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "milestones")
public class Milestone extends Auditable {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    private Proposal proposal;

    @NotNull
    @Size(max=255, message = "Title cannot exceed 255 characters.")
    private String title;

    @NotNull
    @Size(max=2000, message = "Description cannot exceed 2000 characters.")
    private String description;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than zero.")
    private BigDecimal amount;

    @NotNull
    @DecimalMin(value = "0.0", message = "Amount must be greater than zero.")
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.0", message = "Amount must be greater than zero.")
    private BigDecimal bonusAmount = BigDecimal.ZERO;

    @NotNull
    @Min(value = 1, message = "Estimated duration must be at least 1 day.")
    @Column(name = "estimated_duration")
    private Integer estimatedDuration; // in days

    @NotNull
    @Enumerated(EnumType.STRING)
    private MilestoneStatus status = MilestoneStatus.NONE;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.NOT_STARTED;

    @Size(max=2000, message = "Notes cannot exceed 2000 characters.")
    private String notes;

    @NotNull
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.NONE;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="contract_id", nullable = false)
    private Contract contract;

    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "invoice_id", nullable = true)
    private Invoice invoice;

    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "payment_id", nullable = true)
    private Payment payment;

    @PrePersist
    @PreUpdate
    private void applyDefaults() {
        if(plannedStartDate != null && estimatedDuration != null) {
            plannedEndDate = plannedStartDate.plusDays(estimatedDuration);
        }
        if (actualStartDate == null && plannedStartDate != null) {
            actualStartDate = plannedStartDate;
        }
        if(actualStartDate != null && estimatedDuration != null) {
            actualEndDate = actualStartDate.plusDays(estimatedDuration);
        }
    }

}
