package com.jobmatcher.server.mapper;

import com.jobmatcher.server.domain.FreelancerProfile;
import com.jobmatcher.server.domain.Project;
import com.jobmatcher.server.domain.Proposal;
import com.jobmatcher.server.model.ProposalDetailDTO;
import com.jobmatcher.server.model.ProposalRequestDTO;
import com.jobmatcher.server.model.ProposalSummaryDTO;
import org.springframework.stereotype.Component;


@Component
public class ProposalMapper {

    private final FreelancerProfileMapper freelancerMapper;

    public ProposalMapper(FreelancerProfileMapper freelancerMapper) {
        this.freelancerMapper = freelancerMapper;
    }

    public ProposalDetailDTO toDetailDto(Proposal entity) {
        if (entity == null) {
            return null;
        }

        return ProposalDetailDTO.builder()
                .id(entity.getId())
                .projectId(entity.getProject().getId())
                .freelancer(entity.getFreelancer() != null
                        ? freelancerMapper.toFreelancerSummaryDto(entity.getFreelancer())
                        : null)
                .coverLetter(entity.getCoverLetter())
                .amount(entity.getAmount())
                .penaltyAmount(entity.getPenaltyAmount())
                .bonusAmount(entity.getBonusAmount())
                .estimatedDuration(entity.getEstimatedDuration())
                .status(entity.getStatus())
                .paymentStatus(entity.getPaymentStatus())
                .notes(entity.getNotes())
                .plannedStartDate(entity.getPlannedStartDate() != null ? entity.getPlannedStartDate().toString() : null)
                .plannedEndDate(entity.getPlannedEndDate() != null ? entity.getPlannedEndDate().toString() : null)
                .actualStartDate(entity.getActualStartDate() != null ? entity.getActualStartDate().toString() : null)
                .actualEndDate(entity.getActualEndDate() != null ? entity.getActualEndDate().toString() : null)
                .priority(entity.getPriority())
                .createdAt(entity.getCreatedAt())
                .lastUpdate(entity.getLastUpdate())
                .build();
    }

    public ProposalSummaryDTO toSummaryDto(Proposal entity) {
        if (entity == null) {
            return null;
        }

        return ProposalSummaryDTO.builder()
                .id(entity.getId())
                .projectId(entity.getProject() != null ? entity.getProject().getId() : null)
                .freelancer(entity.getFreelancer() != null ? freelancerMapper.toFreelancerSummaryDto(entity.getFreelancer()) : null)
                .coverLetter(entity.getCoverLetter())
                .amount(entity.getAmount())
                .penaltyAmount(entity.getPenaltyAmount())
                .bonusAmount(entity.getBonusAmount())
                .estimatedDuration(entity.getEstimatedDuration())
                .status(entity.getStatus())
                .paymentStatus(entity.getPaymentStatus())
                .notes(entity.getNotes())
                .plannedStartDate(entity.getPlannedStartDate() != null ? entity.getPlannedStartDate().toString() : null)
                .plannedEndDate(entity.getPlannedEndDate() != null ? entity.getPlannedEndDate().toString() : null)
                .actualStartDate(entity.getActualStartDate() != null ? entity.getActualStartDate().toString() : null)
                .actualEndDate(entity.getActualEndDate() != null ? entity.getActualEndDate().toString() : null)
                .priority(entity.getPriority())
                .createdAt(entity.getCreatedAt())
                .lastUpdate(entity.getLastUpdate())
                .build();
    }

    public Proposal toEntity(ProposalRequestDTO dto, Project project, FreelancerProfile freelancer) {
        if (dto == null) {
            return null;
        }

        Proposal entity = new Proposal();
        entity.setProject(project);
        entity.setFreelancer(freelancer);

        if (dto.getCoverLetter() != null) entity.setCoverLetter(dto.getCoverLetter());
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
