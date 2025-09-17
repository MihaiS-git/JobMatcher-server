package com.jobmatcher.server.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
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

    @NotNull
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.NONE;

    @Size(max=2000, message = "Notes cannot exceed 2000 characters.")
    private String notes;

    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.NONE;

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Milestone> milestones = new HashSet<>();

    @PrePersist
    private void prePersist() {
        if (actualStartDate == null) actualStartDate = plannedStartDate;
        if (actualEndDate == null) actualEndDate = plannedEndDate;
    }

}
