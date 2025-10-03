package com.jobmatcher.server.mapper;

import com.jobmatcher.server.domain.Contract;
import com.jobmatcher.server.model.ContactDTO;
import com.jobmatcher.server.model.ContractDetailDTO;
import com.jobmatcher.server.model.ContractSummaryDTO;
import com.jobmatcher.server.model.MilestoneResponseDTO;
import org.springframework.stereotype.Component;

import java.util.Set;

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

    public ContractDetailDTO toDetailDto(Contract entity, ContactDTO customerContact, ContactDTO freelancerContact, Set<MilestoneResponseDTO> milestones) {
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
                .invoiceId(entity.getInvoice() != null ? entity.getInvoice().getId() : null)
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
