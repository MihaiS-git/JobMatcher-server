package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.*;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
public class ProposalService implements IProposalService{

    private final ProposalRepository proposalRepository;
    private final ProjectRepository projectRepository;
    private final FreelancerProfileRepository freelancerRepository;
    private final ProposalMapper proposalMapper;

    public ProposalService(
            ProposalRepository proposalRepository,
            ProjectRepository projectRepository,
            FreelancerProfileRepository freelancerRepository,
            ProposalMapper proposalMapper
    ) {
        this.proposalRepository = proposalRepository;
        this.projectRepository = projectRepository;
        this.freelancerRepository = freelancerRepository;
        this.proposalMapper = proposalMapper;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ProposalSummaryDTO> getProposalsByProjectId(UUID projectId, Pageable pageable, ProposalStatus status) {
        Page<Proposal> proposals;
        if (status != null) {
            proposals = proposalRepository.findByProjectIdAndStatus(projectId, pageable, status);
        } else {
            proposals = proposalRepository.findByProjectId(projectId, pageable);
        }
        return proposals.map(proposalMapper::toSummaryDto);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ProposalSummaryDTO> getProposalsByFreelancerId(UUID freelancerId, Pageable pageable, ProposalStatus status) {
        Page<Proposal> proposals;
        if (status != null) {
            proposals = proposalRepository.findByFreelancerIdAndStatus(freelancerId, pageable, status);
        } else {
            proposals = proposalRepository.findByFreelancerId(freelancerId, pageable);
        }
        return proposals.map(proposalMapper::toSummaryDto);
    }

    @Transactional(readOnly = true)
    @Override
    public ProposalDetailDTO getProposalById(UUID id) {
        return proposalRepository.findById(id)
                .map(proposalMapper::toDetailDto)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));
    }

    @Override
    public ProposalDetailDTO getProposalByFreelancerIdAndProjectId(UUID freelancerId, UUID projectId) {
        return proposalRepository.findByFreelancerIdAndProjectId(freelancerId, projectId)
                .map(proposalMapper::toDetailDto)
                .orElse(null);
    }

    @Override
    public ProposalSummaryDTO createProposal(ProposalRequestDTO requestDTO) {
        log.info("Creating proposal for Project ID: {} by Freelancer ID: {}",
                requestDTO.getProjectId(), requestDTO.getFreelancerId());
        Project project = projectRepository.findById(requestDTO.getProjectId()).orElseThrow(() ->
                new ResourceNotFoundException("Project not found"));
        FreelancerProfile freelancer = freelancerRepository.findById(requestDTO.getFreelancerId()).orElseThrow(() ->
                new ResourceNotFoundException("Freelancer not found"));
        Proposal proposalRequest = proposalMapper.toEntity(requestDTO, project, freelancer);
        Proposal savedProposal = proposalRepository.save(proposalRequest);
        log.info("Created proposal with ID: {}", savedProposal.getId());

        return proposalMapper.toSummaryDto(savedProposal);
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

        if(requestDTO.getStatus() == ProposalStatus.ACCEPTED){
            Project project = projectRepository.findById(existentProposal.getProject().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
            Set<Proposal> otherProposals = project.getProposals().stream()
                    .filter((p) -> !p.getId().equals(id))
                    .collect(Collectors.toSet());
            otherProposals.forEach((p -> p.setStatus(ProposalStatus.REJECTED)));
            proposalRepository.saveAll(otherProposals);
            project.setStatus(ProjectStatus.IN_PROGRESS);
            projectRepository.save(project);
        }

        return proposalMapper.toDetailDto(updatedProposal);
    }


    @Override
    public void deleteProposalById(UUID id) {
        Proposal existentProposal = proposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));
        proposalRepository.delete(existentProposal);
    }
}
