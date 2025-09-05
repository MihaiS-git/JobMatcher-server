package com.jobmatcher.server.mapper;

import com.jobmatcher.server.domain.FreelancerProfile;
import com.jobmatcher.server.domain.Milestone;
import com.jobmatcher.server.domain.Proposal;
import com.jobmatcher.server.model.MilestoneRequestDTO;
import com.jobmatcher.server.model.MilestoneResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class MilestoneMapper {

    public MilestoneResponseDTO toDto(Milestone entity) {
        if (entity == null) {
            return null;
        }

        return MilestoneResponseDTO.builder()
                .id(entity.getId())
                .proposalId(entity.getProposal().getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .amount(entity.getAmount())
                .penaltyAmount(entity.getPenaltyAmount())
                .bonusAmount(entity.getBonusAmount())
                .estimatedDuration(entity.getEstimatedDuration())
                .status(entity.getStatus())
                .paymentStatus(entity.getPaymentStatus())
                .notes(entity.getNotes())
                .plannedStartDate(entity.getPlannedStartDate())
                .plannedEndDate(entity.getPlannedEndDate())
                .actualStartDate(entity.getActualStartDate())
                .actualEndDate(entity.getActualEndDate())
                .priority(entity.getPriority())
                .build();
    }

    public Milestone toEntity(MilestoneRequestDTO dto, Proposal proposal) {
        if (dto == null) {
            return null;
        }

        Milestone entity = new Milestone();
        entity.setProposal(proposal);

        if (dto.getTitle() != null) entity.setTitle(dto.getTitle());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getAmount() != null) entity.setAmount(dto.getAmount());
        if (dto.getPenaltyAmount() != null) entity.setPenaltyAmount(dto.getPenaltyAmount());
        if (dto.getBonusAmount() != null) entity.setBonusAmount(dto.getBonusAmount());
        if (dto.getEstimatedDuration() != null) entity.setEstimatedDuration(dto.getEstimatedDuration());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        if (dto.getPaymentStatus() != null) entity.setPaymentStatus(dto.getPaymentStatus());
        if (dto.getNotes() != null) entity.setNotes(dto.getNotes());
        if (dto.getPlannedStartDate() != null) entity.setPlannedStartDate(dto.getPlannedStartDate());
        if (dto.getPlannedEndDate() != null) entity.setPlannedEndDate(dto.getPlannedEndDate());
        if (dto.getActualStartDate() != null) entity.setActualStartDate(dto.getActualStartDate());
        if (dto.getActualEndDate() != null) entity.setActualEndDate(dto.getActualEndDate());
        if (dto.getPriority() != null) entity.setPriority(dto.getPriority());

        return entity;
    }

}
