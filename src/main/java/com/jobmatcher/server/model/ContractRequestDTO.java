package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.ContractStatus;
import com.jobmatcher.server.domain.Invoice;
import com.jobmatcher.server.domain.Payment;
import com.jobmatcher.server.domain.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Builder
public class ContractRequestDTO {
    private ContractStatus status;
    private Invoice invoice;
    private Payment payment;
    private BigDecimal totalPaid;
    private BigDecimal remainingBalance;
    private PaymentStatus paymentStatus;
    private OffsetDateTime completedAt;
    private OffsetDateTime terminatedAt;
}
