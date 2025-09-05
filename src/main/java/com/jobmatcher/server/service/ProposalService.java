package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.FreelancerProfile;
import com.jobmatcher.server.domain.Project;
import com.jobmatcher.server.domain.Proposal;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.ProposalMapper;
import com.jobmatcher.server.model.ProposalDetailDTO;
import com.jobmatcher.server.model.ProposalRequestDTO;
import com.jobmatcher.server.model.ProposalSummaryDTO;
import com.jobmatcher.server.repository.FreelancerProfileRepository;
import com.jobmatcher.server.repository.ProjectRepository;
import com.jobmatcher.server.repository.ProposalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class ProposalService implements IProposalService{

    private final ProposalRepository proposalRepository;
    private final ProjectRepository projectRepository;
    private final FreelancerProfileRepository freelancerRepository;
    private final ProposalMapper proposalMapper;

    public ProposalService(ProposalRepository proposalRepository, ProjectRepository projectRepository, FreelancerProfileRepository freelancerRepository, ProposalMapper proposalMapper) {
        this.proposalRepository = proposalRepository;
        this.projectRepository = projectRepository;
        this.freelancerRepository = freelancerRepository;
        this.proposalMapper = proposalMapper;
    }

    @Override
    public Page<ProposalSummaryDTO> getProposalsByProjectId(UUID projectId, Pageable pageable) {
        Page<Proposal> proposals = proposalRepository.findByProjectId(projectId, pageable);
        return proposals.map(proposalMapper::toSummaryDto);
    }

    @Override
    public ProposalDetailDTO getProposalById(UUID id) {
        return proposalRepository.findById(id)
                .map(proposalMapper::toDetailDto)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));
    }

    @Override
    public ProposalDetailDTO createProposal(ProposalRequestDTO requestDTO) {
        log.info("Creating proposal for Project ID: {} by Freelancer ID: {}", requestDTO.getProjectId(), requestDTO.getFreelancerId());
        Project project = projectRepository.findById(requestDTO.getProjectId()).orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        FreelancerProfile freelancer = freelancerRepository.findById(requestDTO.getFreelancerId()).orElseThrow(() -> new ResourceNotFoundException("Freelancer not found"));
        Proposal proposalRequest = proposalMapper.toEntity(requestDTO, project, freelancer);
        Proposal savedProposal = proposalRepository.save(proposalRequest);
        log.info("Created proposal with ID: {}", savedProposal.getId());

        return proposalMapper.toDetailDto(savedProposal);
    }

    @Override
    public ProposalDetailDTO updateProposalById(UUID id, ProposalRequestDTO requestDTO) {
        Proposal existentProposal = proposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));

        if (requestDTO.getCoverLetter() != null) {
            existentProposal.setCoverLetter(requestDTO.getCoverLetter());
        }
        if (requestDTO.getAmount() != null) {
            existentProposal.setAmount(requestDTO.getAmount());
        }
        if (requestDTO.getPenaltyAmount() != null) {
            existentProposal.setPenaltyAmount(requestDTO.getPenaltyAmount());
        }
        if (requestDTO.getBonusAmount() != null) {
            existentProposal.setBonusAmount(requestDTO.getBonusAmount());
        }
        if (requestDTO.getEstimatedDuration() != null) {
            existentProposal.setEstimatedDuration(requestDTO.getEstimatedDuration());
        }
        if (requestDTO.getStatus() != null) {
            existentProposal.setStatus(requestDTO.getStatus());
        }
        if (requestDTO.getPaymentStatus() != null) {
            existentProposal.setPaymentStatus(requestDTO.getPaymentStatus());
        }
        if (requestDTO.getNotes() != null) {
            existentProposal.setNotes(requestDTO.getNotes());
        }
        if (requestDTO.getPlannedStartDate() != null) {
            existentProposal.setPlannedStartDate(requestDTO.getPlannedStartDate());
        }
        if (requestDTO.getPlannedEndDate() != null) {
            existentProposal.setPlannedEndDate(requestDTO.getPlannedEndDate());
        }
        if (requestDTO.getActualStartDate() != null) {
            existentProposal.setActualStartDate(requestDTO.getActualStartDate());
        }
        if (requestDTO.getActualEndDate() != null) {
            existentProposal.setActualEndDate(requestDTO.getActualEndDate());
        }
        if (requestDTO.getPriority() != null) {
            existentProposal.setPriority(requestDTO.getPriority());
        }
        Proposal updatedProposal = proposalRepository.save(existentProposal);

        return proposalMapper.toDetailDto(updatedProposal);
    }

    @Override
    public void deleteProposalById(UUID id) {
        Proposal existentProposal = proposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));
        proposalRepository.delete(existentProposal);
    }
}
