package com.jobmatcher.server.mapper;

import com.jobmatcher.server.domain.Contract;
import com.jobmatcher.server.domain.Invoice;
import com.jobmatcher.server.model.*;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ContractMapper {

    public ContractSummaryDTO toSummaryDto(Contract entity) {
        if(entity == null) {
            return null;
        }
        return ContractSummaryDTO.builder()
                .id(entity.getId())
                .customerName(entity.getCustomer().getUser().getFirstName() + " " + entity.getCustomer().getUser().getLastName())
                .freelancerName(entity.getFreelancer().getUser().getFirstName() + " " + entity.getFreelancer().getUser().getLastName())
                .status(entity.getStatus())
                .title(entity.getTitle())
                .amount(entity.getAmount())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .paymentType(entity.getPaymentType())
                .build();
    }

    public ContractDetailDTO toDetailDto(Contract entity, ContactDTO customerContact, ContactDTO freelancerContact, Set<InvoiceSummaryDTO> invoices, Set<MilestoneResponseDTO> milestones) {
        if(entity == null) {
            return null;
        }
        return ContractDetailDTO.builder()
                .id(entity.getId())
                .proposalId(entity.getProposal().getId())
                .projectId(entity.getProject().getId())
                .customerId(entity.getCustomer().getId())
                .freelancerId(entity.getFreelancer().getId())
                .customerName(entity.getCustomer().getUser().getFirstName() + " " + entity.getCustomer().getUser().getLastName())
                .freelancerName(entity.getFreelancer().getUser().getFirstName() + " " + entity.getFreelancer().getUser().getLastName())
                .customerContact(customerContact)
                .freelancerContact(freelancerContact)
                .status(entity.getStatus())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .amount(entity.getAmount())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .paymentType(entity.getPaymentType())
                .milestones(milestones)
                .invoices(invoices)
                .paymentId(entity.getPayment() != null ? entity.getPayment().getId() : null)
                .totalPaid(entity.getTotalPaid())
                .remainingBalance(entity.getRemainingBalance())
                .paymentStatus(entity.getPaymentStatus())
                .signedAt(entity.getSignedAt())
                .completedAt(entity.getCompletedAt())
                .terminatedAt(entity.getTerminatedAt())
                .build();
    }
}
