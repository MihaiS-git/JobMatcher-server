package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.ContractStatus;
import com.jobmatcher.server.domain.PaymentType;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;


@Getter
@Setter
public class ContractFilterDTO {
    private String customerName;
    private String freelancerName;
    private ContractStatus status;
    private PaymentType paymentType;
    private String searchTerm;
}
