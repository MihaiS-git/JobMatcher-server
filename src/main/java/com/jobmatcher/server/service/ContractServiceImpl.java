package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.ContractMapper;
import com.jobmatcher.server.mapper.MilestoneMapper;
import com.jobmatcher.server.model.*;
import com.jobmatcher.server.repository.ContractRepository;
import com.jobmatcher.server.specification.ContractSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Transactional(rollbackFor = Exception.class)
@Service
public class ContractServiceImpl implements IContractService {

    private final ContractRepository contractRepository;
    private final ContractMapper contractMapper;
    private final ICustomerProfileService customerService;
    private final IFreelancerProfileService freelancerService;
    private final IUserService userService;
    private final MilestoneMapper milestoneMapper;
    private final IProjectService projectService;
    private final IProposalService proposalService;
    private final JwtService jwtService;

    public ContractServiceImpl(
            ContractRepository contractRepository,
            ContractMapper contractMapper,
            ICustomerProfileService customerService,
            IFreelancerProfileService freelancerService,
            IUserService userService,
            MilestoneMapper milestoneMapper,
            IProjectService projectService,
            IProposalService proposalService,
            JwtService jwtService
    ) {
        this.contractRepository = contractRepository;
        this.contractMapper = contractMapper;
        this.customerService = customerService;
        this.freelancerService = freelancerService;
        this.userService = userService;
        this.milestoneMapper = milestoneMapper;
        this.projectService = projectService;
        this.proposalService = proposalService;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ContractSummaryDTO> getAllContractsByProfileId(
            String authHeader,
            String profileId,
            Pageable pageable,
            ContractFilterDTO filter
    ) {
        String token = authHeader.replace("Bearer ", "").trim();
        Role role = jwtService.extractRole(token);

        return contractRepository.findAll(ContractSpecifications.withFiltersAndRole(filter, role, profileId), pageable).map(contractMapper::toSummaryDto);
    }

    @Transactional(readOnly = true)
    @Override
    public ContractDetailDTO getContractById(UUID contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract with ID " + contractId + " not found."));
        ContractDetailData contractDetailData = getContractDetailData(contract);

        return contractMapper.toDetailDto(
                contract,
                contractDetailData.customerContact,
                contractDetailData.freelancerContact,
                contractDetailData.milestonesList
        );
    }

    @Transactional(readOnly = true)
    @Override
    public ContractDetailDTO getContractByProjectId(UUID projectId) {
        Contract contract = contractRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract for project ID " + projectId + " not found."));
        ContractDetailData contractDetailData = getContractDetailData(contract);

        return contractMapper.toDetailDto(
                contract,
                contractDetailData.customerContact,
                contractDetailData.freelancerContact,
                contractDetailData.milestonesList
        );
    }

    @Override
    public ContractDetailDTO updateContractById(UUID contractId, ContractRequestDTO request) {
        Contract existentContract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract with ID " + contractId + " not found."));
        Contract updatedContract = updateExistentContract(request, existentContract);

        ContractDetailData contractDetailData = getContractDetailData(updatedContract);

        return contractMapper.toDetailDto(
                updatedContract,
                contractDetailData.customerContact(),
                contractDetailData.freelancerContact(),
                contractDetailData.milestonesList()
        );
    }

    @Override
    public void deleteContractById(UUID contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract with ID " + contractId + " not found."));
        if (contract.getInvoice() != null || contract.getMilestones() != null) {
            throw new IllegalStateException("Cannot delete contract with associated milestones or invoice.");
        }

        // Revert project and proposals status
        Project project = contract.getProject();
        if (project != null) {
            Set<Proposal> proposals = project.getProposals();
            if (!proposals.isEmpty()) {
                for (Proposal p : proposals) {
                    ProposalRequestDTO proposalRequestDTO = ProposalRequestDTO.builder()
                            .status(ProposalStatus.PENDING)
                            .build();
                    proposalService.updateProposalById(p.getId(), proposalRequestDTO);
                }
                Proposal contractProposal = contract.getProposal();
                if (contractProposal != null) {
                    contractProposal.setStatus(ProposalStatus.REJECTED);
                }
            }

            ProjectRequestDTO projectRequestDTO = ProjectRequestDTO.builder()
                    .freelancerId(Optional.empty())
                    .contractId(Optional.empty())
                    .status(ProjectStatus.PROPOSALS_RECEIVED)
                    .acceptedProposalId(Optional.empty())
                    .build();
            projectService.updateProject(project.getId(), projectRequestDTO);
        }
        contractRepository.delete(contract);
    }


    private record ContractDetailData(ContactDTO customerContact, ContactDTO freelancerContact,
                                      Set<MilestoneResponseDTO> milestonesList) {
    }

    private static ContactDTO getContact(UserResponseDTO user) {
        return ContactDTO.builder()
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddressResponseDto())
                .build();
    }

    private ContractDetailData getContractDetailData(Contract contract) {
        UUID customerId = contract.getProject().getCustomer().getId();
        UUID freelancerId = contract.getProject().getFreelancer().getId();
        CustomerDetailDTO customer = customerService.getCustomerProfileById(customerId);
        FreelancerDetailDTO freelancer = freelancerService.getFreelancerProfileById(freelancerId);
        UserResponseDTO customerUser = userService.getUserById(customer.getUserId());
        UserResponseDTO freelancerUser = userService.getUserById(freelancer.getUserId());
        ContactDTO customerContact = getContact(customerUser);
        ContactDTO freelancerContact = getContact(freelancerUser);
        Set<Milestone> milestones = contract.getMilestones();
        Set<MilestoneResponseDTO> milestonesList = milestones != null
                ? milestones.stream().map(milestoneMapper::toDto).collect(Collectors.toSet())
                : Set.of();
        return new ContractDetailData(customerContact, freelancerContact, milestonesList);
    }

    private Contract updateExistentContract(ContractRequestDTO request, Contract existentContract) {
        if (request.getStatus() != null) {
            existentContract.setStatus(request.getStatus());
            ProjectRequestDTO projectRequestDTO;
            switch (request.getStatus()) {
                case ContractStatus.ACTIVE -> projectRequestDTO = ProjectRequestDTO.builder()
                        .status(ProjectStatus.IN_PROGRESS)
                        .build();
                case ContractStatus.CANCELLED -> projectRequestDTO = ProjectRequestDTO.builder()
                        .status(ProjectStatus.CANCELLED)
                        .build();
                case ContractStatus.COMPLETED -> projectRequestDTO = ProjectRequestDTO.builder()
                        .status(ProjectStatus.COMPLETED)
                        .build();
                case ContractStatus.TERMINATED -> projectRequestDTO = ProjectRequestDTO.builder()
                        .status(ProjectStatus.TERMINATED)
                        .build();
                case ContractStatus.ON_HOLD -> projectRequestDTO = ProjectRequestDTO.builder()
                        .status(ProjectStatus.ON_HOLD)
                        .build();
                default -> projectRequestDTO = null;
            }
            projectService.updateProject(existentContract.getProject().getId(), projectRequestDTO);
        }
        if (request.getInvoice() != null) {
            existentContract.setInvoice(request.getInvoice());
        }
        if (request.getPayment() != null) {
            existentContract.setPayment(request.getPayment());
        }
        if (request.getTotalPaid() != null) {
            existentContract.setTotalPaid(request.getTotalPaid());
        }
        if (request.getRemainingBalance() != null) {
            existentContract.setRemainingBalance(request.getRemainingBalance());
        }
        if (request.getPaymentStatus() != null) {
            existentContract.setPaymentStatus(request.getPaymentStatus());
        }
        if (request.getCompletedAt() != null) {
            existentContract.setCompletedAt(request.getCompletedAt());
        }
        if (request.getTerminatedAt() != null) {
            existentContract.setTerminatedAt(request.getTerminatedAt());
        }
        return contractRepository.save(existentContract);
    }
}
