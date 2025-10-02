package com.jobmatcher.server.model;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class PaymentRequestDTO {

    @NotNull
    private UUID invoiceId;

    private String notes;
}
