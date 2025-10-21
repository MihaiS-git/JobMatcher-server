package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.InvoiceStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InvoiceStatusRequestDTO {

    private InvoiceStatus status;

}
