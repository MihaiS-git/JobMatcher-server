package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.ProposalMapper;
import com.jobmatcher.server.model.*;
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
    private final IProjectService projectService;

    public ProposalServiceImpl(
            ProposalRepository proposalRepository,
            ProjectRepository projectRepository,
            FreelancerProfileRepository freelancerRepository,
            ProposalMapper proposalMapper,
            ContractRepository contractRepository,
            IProjectService projectService
            ) {
        this.proposalRepository = proposalRepository;
        this.projectRepository = projectRepository;
        this.freelancerRepository = freelancerRepository;
        this.proposalMapper = proposalMapper;
        this.contractRepository = contractRepository;
        this.projectService = projectService;
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

        if (project.getStatus() != ProjectStatus.OPEN) {
            throw new IllegalStateException("Cannot submit proposal to a project that is not open for proposals.");
        }

        FreelancerProfile freelancer = freelancerRepository.findById(requestDTO.getFreelancerId()).orElseThrow(() ->
                new ResourceNotFoundException("Freelancer not found"));

        boolean exists = proposalRepository.existsByFreelancerIdAndProjectId(
                requestDTO.getFreelancerId(), requestDTO.getProjectId());

        if (exists) {
            throw new IllegalStateException("You have already submitted a proposal for this project. You can update it instead.");
        }

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

        Proposal updatedProposal = proposalRepository.save(existentProposal);

        return proposalMapper.toDetailDto(updatedProposal);
    }

    @Override
    public ProposalDetailDTO updateProposalStatusById(UUID id, ProposalStatusRequestDTO requestDTO) {
        Proposal existentProposal = proposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));

        switch (requestDTO.getStatus()) {
            case ACCEPTED -> {
                log.info("Accepting proposal with ID: {}", id);

                existentProposal.setStatus(ProposalStatus.ACCEPTED);

                Project project = existentProposal.getProject();
                FreelancerProfile freelancer = existentProposal.getFreelancer();
                Contract contract = getContract(existentProposal,
                        existentProposal.getProject(),
                        existentProposal.getFreelancer());
                Contract savedContract = contractRepository.save(contract);

                existentProposal.setContract(savedContract);

                project.setAcceptedProposal(existentProposal);
                project.setContract(savedContract);
                project.setFreelancer(freelancer);
                project.setStatus(ProjectStatus.IN_PROGRESS);

                freelancer.getContracts().add(savedContract);

                proposalRepository.save(existentProposal);
                projectRepository.save(project);
                freelancerRepository.save(freelancer);

                log.info("Created contract with ID: {} for proposal ID: {}", contract.getId(), id);
            }
            case REJECTED -> {
                log.info("Rejecting proposal with ID: {}", id);
                existentProposal.setStatus(ProposalStatus.REJECTED);
            }
            case WITHDRAWN -> {
                log.info("Withdrawing proposal with ID: {}", id);
                existentProposal.setStatus(ProposalStatus.WITHDRAWN);
                ProjectStatusUpdateDTO projectStatusUpdateDTO = ProjectStatusUpdateDTO.builder()
                        .status(ProjectStatus.OPEN)
                        .build();
                projectService.updateProjectStatus(
                        existentProposal.getProject().getId(),
                        projectStatusUpdateDTO
                );
            }
            default -> {
                log.info("Pending proposal with ID: {}", id);
                existentProposal.setStatus(ProposalStatus.PENDING);
            }
        };

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
                " for the project " + project.getTitle());
        contract.setAmount(existentProposal.getAmount());
        contract.setRemainingBalance(existentProposal.getAmount().subtract(contract.getTotalPaid()));
        contract.setStartDate(existentProposal.getPlannedStartDate());
        contract.setEndDate(existentProposal.getPlannedEndDate());
        return contract;
    }

    @Override
    public void deleteProposalById(UUID id) {
        Proposal existentProposal = proposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));
        if (existentProposal.getContract() != null) {
            throw new IllegalStateException("Cannot delete a proposal that has an associated contract.");
        }
        proposalRepository.delete(existentProposal);
    }
}
