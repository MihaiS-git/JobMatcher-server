package com.jobmatcher.server.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "proposals")
public class Proposal extends Auditable{

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freelancer_id", nullable = false)
    private FreelancerProfile freelancer;

    @Column(name = "cover_letter", length = 2000)
    private String coverLetter;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @NotNull
    @DecimalMin(value = "0.0", message = "Amount must be greater than zero.")
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.0", message = "Amount must be greater than zero.")
    private BigDecimal bonusAmount = BigDecimal.ZERO;

    @NotNull
    @Min(1)
    @Column(name = "estimated_duration", nullable = false) // in days
    private Integer estimatedDuration;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ProposalStatus status = ProposalStatus.PENDING;

    @Size(max=2000, message = "Notes cannot exceed 2000 characters.")
    private String notes;

    @NotNull
    @Column(name = "planned_start_date", nullable = false)
    private OffsetDateTime plannedStartDate;

    @Column(name = "planned_end_date", nullable = false)
    private OffsetDateTime plannedEndDate;

    @Column(name = "actual_start_date", nullable = false)
    private OffsetDateTime actualStartDate;

    @Column(name = "actual_end_date", nullable = false)
    private OffsetDateTime actualEndDate;

    @OneToOne(mappedBy = "proposal", fetch = FetchType.LAZY)
    private Contract contract;

    @PrePersist
    private void prePersistDefaults() {
        if (plannedStartDate == null) plannedStartDate = OffsetDateTime.now();
        if (estimatedDuration == null) estimatedDuration = 7;

        if (plannedEndDate == null)
            plannedEndDate = plannedStartDate.plusDays(estimatedDuration);

        if (actualStartDate == null) actualStartDate = plannedStartDate;
        if (actualEndDate == null)
            actualEndDate = actualStartDate.plusDays(estimatedDuration);
    }
}
