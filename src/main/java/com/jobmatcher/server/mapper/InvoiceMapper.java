package com.jobmatcher.server.mapper;

import com.jobmatcher.server.domain.Invoice;
import com.jobmatcher.server.model.*;
import org.springframework.stereotype.Component;

@Component
public class InvoiceMapper {

    public InvoiceDetailDTO toDetailDto(Invoice entity, ContractDetailDTO contractDto, MilestoneResponseDTO milestoneDto) {
         if (entity == null) {
            return null;
        }

        return InvoiceDetailDTO.builder()
                .id(entity.getId())
                .contract(contractDto)
                .milestone(milestoneDto)
                .amount(entity.getAmount())
                .issuedAt(entity.getIssuedAt())
                .dueDate(entity.getDueDate())
                .status(entity.getStatus())
                .paymentId(entity.getPayment() != null ? entity.getPayment().getId() : null)
                .build();
    }

    public InvoiceSummaryDTO toSummaryDto(Invoice entity, ContractSummaryDTO contractDto, MilestoneResponseDTO milestoneDto) {
        if (entity == null) {
            return null;
        }

        return InvoiceSummaryDTO.builder()
                .id(entity.getId())
                .contract(contractDto)
                .milestone(milestoneDto)
                .amount(entity.getAmount())
                .issuedAt(entity.getIssuedAt())
                .dueDate(entity.getDueDate())
                .status(entity.getStatus())
                .build();
    }
}
