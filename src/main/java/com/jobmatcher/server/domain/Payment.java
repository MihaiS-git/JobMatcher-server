package com.jobmatcher.server.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "payments")
public class Payment extends Auditable {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @OneToOne(mappedBy = "payment", fetch = FetchType.LAZY)
    private Contract contract;

    @OneToOne(mappedBy = "payment", fetch = FetchType.LAZY)
    private Milestone milestone;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false, unique = true)
    private Invoice invoice;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than zero.")
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PAID;

    @NotNull
    private OffsetDateTime paidAt;

    @Size(max = 4000, message = "Notes cannot exceed 4000 characters.")
    private String notes;

    private String stripePaymentIntentId;
}
