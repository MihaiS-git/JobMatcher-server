package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class PaymentRequestDTO {

    private UUID invoiceId;
    private PaymentStatus status;
    private String notes;

}
