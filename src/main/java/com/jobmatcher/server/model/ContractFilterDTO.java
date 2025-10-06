package com.jobmatcher.server.model;

import com.jobmatcher.server.domain.ContractStatus;
import com.jobmatcher.server.domain.PaymentStatus;
import com.jobmatcher.server.domain.PaymentType;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ContractFilterDTO {
    private UUID freelancerId;
    private UUID customerId;
    private UUID projectId;
    private UUID proposalId;
    private ContractStatus status;
    private PaymentStatus paymentStatus;
    private PaymentType paymentType;
    private String searchTerm;
}
