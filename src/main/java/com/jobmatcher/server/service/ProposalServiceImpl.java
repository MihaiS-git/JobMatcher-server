package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.ProposalMapper;
import com.jobmatcher.server.model.ProposalDetailDTO;
import com.jobmatcher.server.model.ProposalRequestDTO;
import com.jobmatcher.server.model.ProposalSummaryDTO;
import com.jobmatcher.server.repository.ContractRepository;
import com.jobmatcher.server.repository.FreelancerProfileRepository;
import com.jobmatcher.server.repository.ProjectRepository;
import com.jobmatcher.server.repository.ProposalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
public class ProposalServiceImpl implements IProposalService {

    private final ProposalRepository proposalRepository;
    private final ProjectRepository projectRepository;
    private final FreelancerProfileRepository freelancerRepository;
    private final ProposalMapper proposalMapper;
    private final ContractRepository contractRepository;

    public ProposalServiceImpl(
            ProposalRepository proposalRepository,
            ProjectRepository projectRepository,
            FreelancerProfileRepository freelancerRepository,
            ProposalMapper proposalMapper,
            ContractRepository contractRepository
    ) {
        this.proposalRepository = proposalRepository;
        this.projectRepository = projectRepository;
        this.freelancerRepository = freelancerRepository;
        this.proposalMapper = proposalMapper;
        this.contractRepository = contractRepository;
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
        if (project.getStatus() == ProjectStatus.IN_PROGRESS ||
                project.getStatus() == ProjectStatus.COMPLETED ||
                project.getStatus() == ProjectStatus.CANCELLED ||
                project.getStatus() == ProjectStatus.NONE
        ) {
            throw new IllegalStateException("Cannot submit proposal to a project that is not open for proposals.");
        }

        FreelancerProfile freelancer = freelancerRepository.findById(requestDTO.getFreelancerId()).orElseThrow(() ->
                new ResourceNotFoundException("Freelancer not found"));

        boolean exists = proposalRepository.existsByFreelancerIdAndProjectId(
                requestDTO.getFreelancerId(), requestDTO.getProjectId());
        if (exists) {
            throw new IllegalStateException("You have already submitted a proposal for this project.");
        }

        Proposal proposalRequest = proposalMapper.toEntity(requestDTO, project, freelancer);
        Proposal savedProposal = proposalRepository.save(proposalRequest);
        log.info("Created proposal with ID: {}", savedProposal.getId());

        if (project.getStatus() == ProjectStatus.OPEN) {
            project.setStatus(ProjectStatus.PROPOSALS_RECEIVED);
            projectRepository.save(project);
        }

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
            if (existentProposal.getStatus() != requestDTO.getStatus()) {
                Project project = projectRepository.findById(existentProposal.getProject().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
                if(project.getStatus() == ProjectStatus.COMPLETED ||
                        project.getStatus() == ProjectStatus.CANCELLED ||
                        project.getStatus() == ProjectStatus.NONE) {
                    throw new IllegalStateException("Cannot change proposal status for a project that is completed, cancelled, or none.");
                }

                if (requestDTO.getStatus() == ProposalStatus.ACCEPTED) {
                    FreelancerProfile freelancer = freelancerRepository.findById(existentProposal.getFreelancer().getId())
                            .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found"));
                    project.setFreelancer(freelancer);
                    project.setAcceptedProposal(proposalRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Proposal not found")));

                    Contract contract = getContract(existentProposal, project, freelancer);
                    contractRepository.save(contract);

                    proposalRepository.rejectOtherPendingProposals(project.getId(), existentProposal.getId(), ProposalStatus.REJECTED);

                    project.setStatus(ProjectStatus.IN_PROGRESS);
                } else if (requestDTO.getStatus() == ProposalStatus.WITHDRAWN) {
                    if (project.getFreelancer() != null &&
                            project.getFreelancer().getId().equals(existentProposal.getFreelancer().getId())) {
                        project.setFreelancer(null);
                    }
                    project.setStatus(ProjectStatus.PROPOSALS_RECEIVED);
                }
                projectRepository.save(project);
            }

            existentProposal.setStatus(requestDTO.getStatus());
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

    private static Contract getContract(
            Proposal existentProposal,
            Project project,
            FreelancerProfile freelancer
    ) {
        Contract contract = new Contract();
        contract.setProposal(existentProposal);
        contract.setProject(project);
        contract.setCustomer(project.getCustomer());
        contract.setFreelancer(freelancer);
        contract.setTitle("Contract for Project: " + project.getTitle());
        contract.setDescription("Contract between " +
                project.getCustomer().getUser().getFirstName() + " " +
                project.getCustomer().getUser().getLastName() + " and " +
                freelancer.getUser().getFirstName() + " " +
                freelancer.getUser().getLastName() +
                " for the project "+ project.getTitle());
        contract.setAmount(existentProposal.getAmount());
        contract.setStartDate(existentProposal.getPlannedStartDate());
        contract.setEndDate(existentProposal.getPlannedEndDate());
        contract.setPaymentType(project.getPaymentType());
        contract.setMilestones(existentProposal.getMilestones());
        return contract;
    }

    @Override
    public void deleteProposalById(UUID id) {
        Proposal existentProposal = proposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));
        if(existentProposal.getContract() != null) {
            throw new IllegalStateException("Cannot delete a proposal that has an associated contract.");
        }
        proposalRepository.delete(existentProposal);
    }
}
