package com.jobmatcher.server.mapper;

import com.jobmatcher.server.domain.Payment;
import com.jobmatcher.server.model.*;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentDetailDTO toDetailDto(
            Payment entity,
            ContractSummaryDTO contract,
            MilestoneResponseDTO milestone,
            InvoiceSummaryDTO invoice
    ){
        if(entity == null) return null;

        return PaymentDetailDTO.builder()
                .id(entity.getId())
                .contract(contract)
                .milestone(milestone)
                .invoice(invoice)
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .dueDate(entity.getDueDate())
                .paidAt(entity.getPaidAt())
                .notes(entity.getNotes())
                .build();
    }

    public PaymentSummaryDTO toSummaryDto(
            Payment entity,
            ContractSummaryDTO contract,
            MilestoneResponseDTO milestone,
            InvoiceSummaryDTO invoice
    ){
        if(entity == null) return null;

        return PaymentSummaryDTO.builder()
                .id(entity.getId())
                .contract(contract)
                .milestone(milestone)
                .invoice(invoice)
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .dueDate(entity.getDueDate())
                .paidAt(entity.getPaidAt())
                .build();
    }
}
