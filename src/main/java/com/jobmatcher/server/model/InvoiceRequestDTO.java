package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.Payment;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class InvoiceRequestDTO {

    @NotNull
    private UUID contractId;

    private UUID milestoneId;

    private Payment payment;

}
