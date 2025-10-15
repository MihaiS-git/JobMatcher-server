package com.jobmatcher.server.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "contracts")
public class Contract extends Auditable {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @NotNull
    @OneToOne(mappedBy = "contract", fetch = FetchType.LAZY)
    private Proposal proposal;

    @NotNull
    @OneToOne(mappedBy = "contract", fetch = FetchType.LAZY)
    private Project project;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerProfile customer;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freelancer_id", nullable = false)
    private FreelancerProfile freelancer;

    @Enumerated(EnumType.STRING)
    private ContractStatus status = ContractStatus.ACTIVE;

    @NotBlank
    @Size(max=255)
    @Column(name = "title", nullable = false)
    private String title;

    @NotBlank
    @Size(max=2000)
    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @NotNull
    private OffsetDateTime startDate;

    @NotNull
    private OffsetDateTime endDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Milestone> milestones = new HashSet<>();

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Invoice> invoices = new HashSet<>();

    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "payment_id", unique = true, nullable = true)
    private Payment payment;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal totalPaid = BigDecimal.ZERO;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal remainingBalance = BigDecimal.ZERO;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.NOT_STARTED;

    @NotNull
    private OffsetDateTime signedAt = OffsetDateTime.now();

    private OffsetDateTime completedAt;
    private OffsetDateTime terminatedAt;

}
