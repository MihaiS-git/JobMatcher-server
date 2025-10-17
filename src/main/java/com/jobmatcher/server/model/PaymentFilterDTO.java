package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.PaymentStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentFilterDTO {
    private String contractId;
    private String invoiceId;
    private PaymentStatus status;
    private String searchTerm;
}
