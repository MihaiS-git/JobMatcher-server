package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.ContractMapper;
import com.jobmatcher.server.mapper.InvoiceMapper;
import com.jobmatcher.server.mapper.MilestoneMapper;
import com.jobmatcher.server.model.*;
import com.jobmatcher.server.repository.*;
import com.jobmatcher.server.specification.ContractSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Transactional(rollbackFor = Exception.class)
@Service
public class ContractServiceImpl implements IContractService {

    private final ContractRepository contractRepository;
    private final ContractMapper contractMapper;
    private final InvoiceMapper invoiceMapper;
    private final ICustomerProfileService customerService;
    private final IFreelancerProfileService freelancerService;
    private final IUserService userService;
    private final MilestoneMapper milestoneMapper;
    private final IProjectService projectService;
    private final IProposalService proposalService;
    private final JwtService jwtService;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final FreelancerProfileRepository freelancerProfileRepository;
    private final CustomerProfileRepository customerProfileRepository;

    public ContractServiceImpl(
            ContractRepository contractRepository,
            ContractMapper contractMapper, InvoiceMapper invoiceMapper,
            ICustomerProfileService customerService,
            IFreelancerProfileService freelancerService,
            IUserService userService,
            MilestoneMapper milestoneMapper,
            IProjectService projectService,
            IProposalService proposalService,
            JwtService jwtService,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            FreelancerProfileRepository freelancerProfileRepository,
            CustomerProfileRepository customerProfileRepository
    ) {
        this.contractRepository = contractRepository;
        this.contractMapper = contractMapper;
        this.invoiceMapper = invoiceMapper;
        this.customerService = customerService;
        this.freelancerService = freelancerService;
        this.userService = userService;
        this.milestoneMapper = milestoneMapper;
        this.projectService = projectService;
        this.proposalService = proposalService;
        this.jwtService = jwtService;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.freelancerProfileRepository = freelancerProfileRepository;
        this.customerProfileRepository = customerProfileRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ContractSummaryDTO> getAllContracts(
            String token,
            Pageable pageable,
            ContractFilterDTO filter
    ) {
        User user = getUser(token);
        Role role = user.getRole();

        UUID profileId = switch (role) {
            case CUSTOMER -> getCustomerId(user.getId());
            case STAFF -> getFreelancerId(user.getId());
            default -> null;
        };

        return contractRepository.findAll(ContractSpecifications.withFiltersAndRole(filter, role, profileId), pageable)
                .map(contractMapper::toSummaryDto);
    }

    private User getUser(String token) {
        String email = jwtService.extractUsername(token);
        return userService.getUserByEmail(email);
    }

    private UUID getFreelancerId(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return freelancerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer profile not found for user: " + userId))
                .getId();
    }

    private UUID getCustomerId(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        CustomerProfile customer = customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for user: " + userId));
        return customer.getId();
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
                contractDetailData.invoicesList,
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
                contractDetailData.invoicesList,
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
                contractDetailData.invoicesList(),
                contractDetailData.milestonesList()
        );
    }

    @Override
    public void deleteContractById(UUID contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract with ID " + contractId + " not found."));
        if (!contract.getInvoices().isEmpty() || !contract.getMilestones().isEmpty()) {
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
                    .freelancerId(null)
                    .contractId(null)
                    .status(ProjectStatus.PROPOSALS_RECEIVED)
                    .acceptedProposalId(null)
                    .build();
            projectService.updateProject(project.getId(), projectRequestDTO);
        }
        contractRepository.delete(contract);
    }


    private record ContractDetailData(
            ContactDTO customerContact,
            ContactDTO freelancerContact,
            Set<InvoiceSummaryDTO> invoicesList,
            Set<MilestoneResponseDTO> milestonesList
    ) {

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

        ContractSummaryDTO contractSummary = contractMapper.toSummaryDto(contract);

        Set<Invoice> invoices = contract.getInvoices();
        Set<InvoiceSummaryDTO> invoicesList = invoices != null
                ? invoices.stream().map((i) ->
                invoiceMapper.toSummaryDto(
                        i,
                        contractSummary,
                        i.getMilestone() != null ? milestoneMapper.toDto(i.getMilestone()) : null)).collect(Collectors.toSet())
                : Set.of();
        Set<Milestone> milestones = contract.getMilestones();
        Set<MilestoneResponseDTO> milestonesList = milestones != null
                ? milestones.stream().map(milestoneMapper::toDto).collect(Collectors.toSet())
                : Set.of();
        return new ContractDetailData(customerContact, freelancerContact, invoicesList, milestonesList);
    }

    private Contract updateExistentContract(ContractRequestDTO request, Contract existentContract) {
        if (request.getStatus() != null) {
            existentContract.setStatus(request.getStatus());
            ProjectStatusUpdateDTO projectRequestDTO;
            switch (request.getStatus()) {
                case ContractStatus.ACTIVE -> projectRequestDTO = ProjectStatusUpdateDTO.builder()
                        .status(ProjectStatus.IN_PROGRESS)
                        .build();
                case ContractStatus.CANCELLED -> projectRequestDTO = ProjectStatusUpdateDTO.builder()
                        .status(ProjectStatus.CANCELLED)
                        .build();
                case ContractStatus.COMPLETED -> projectRequestDTO = ProjectStatusUpdateDTO.builder()
                        .status(ProjectStatus.COMPLETED)
                        .build();
                case ContractStatus.TERMINATED -> projectRequestDTO = ProjectStatusUpdateDTO.builder()
                        .status(ProjectStatus.TERMINATED)
                        .build();
                case ContractStatus.ON_HOLD -> projectRequestDTO = ProjectStatusUpdateDTO.builder()
                        .status(ProjectStatus.ON_HOLD)
                        .build();
                default -> projectRequestDTO = null;
            }
            projectService.updateProjectStatus(existentContract.getProject().getId(), projectRequestDTO);
        }
        if (request.getInvoiceId() != null) {
            Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice with ID " +
                            request.getInvoiceId() + " not found."));
            existentContract.getInvoices().add(invoice);
        }
        if (request.getPaymentId() != null) {
            Payment payment = paymentRepository.findById(request.getPaymentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment with ID " +
                            request.getPaymentId() + " not found."));
            existentContract.setPayment(payment);
        }
        if (request.getTotalPaid() != null) {
            existentContract.setTotalPaid(request.getTotalPaid());
            existentContract.setRemainingBalance(existentContract.getAmount().subtract(request.getTotalPaid()));
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
