package com.jobmatcher.server.model;

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

}
