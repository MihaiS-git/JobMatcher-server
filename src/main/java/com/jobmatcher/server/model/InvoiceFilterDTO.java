package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.InvoiceStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class InvoiceFilterDTO {
    private UUID contractId;
    private InvoiceStatus status;
    private String searchTerm;
}
