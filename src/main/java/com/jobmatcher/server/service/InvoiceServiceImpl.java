package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.AddressMapper;
import com.jobmatcher.server.mapper.ContractMapper;
import com.jobmatcher.server.mapper.InvoiceMapper;
import com.jobmatcher.server.mapper.MilestoneMapper;
import com.jobmatcher.server.model.*;
import com.jobmatcher.server.repository.*;
import com.jobmatcher.server.specification.InvoiceSpecifications;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
public class InvoiceServiceImpl implements IInvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final ContractMapper contractMapper;
    private final MilestoneMapper milestoneMapper;
    private final JwtService jwtService;
    private final ContractRepository contractRepository;
    private final MilestoneRepository milestoneRepository;
    private final IContractService contractService;
    private final IMilestoneService milestoneService;
    private final AddressMapper addressMapper;
    private final IUserService userService;
    private final FreelancerProfileRepository freelancerProfileRepository;
    private final CustomerProfileRepository customerProfileRepository;

    public InvoiceServiceImpl(
            InvoiceRepository invoiceRepository,
            InvoiceMapper invoiceMapper,
            ContractMapper contractMapper,
            MilestoneMapper milestoneMapper,
            JwtService jwtService,
            ContractRepository contractRepository,
            MilestoneRepository milestoneRepository,
            IContractService contractService,
            IMilestoneService milestoneService,
            AddressMapper addressMapper,
            IUserService userService,
            FreelancerProfileRepository freelancerProfileRepository,
            CustomerProfileRepository customerProfileRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceMapper = invoiceMapper;
        this.contractMapper = contractMapper;
        this.milestoneMapper = milestoneMapper;
        this.jwtService = jwtService;
        this.contractRepository = contractRepository;
        this.milestoneRepository = milestoneRepository;
        this.contractService = contractService;
        this.milestoneService = milestoneService;
        this.addressMapper = addressMapper;
        this.userService = userService;
        this.freelancerProfileRepository = freelancerProfileRepository;
        this.customerProfileRepository = customerProfileRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<InvoiceSummaryDTO> getAllInvoices(
            String token,
            Pageable pageable,
            InvoiceFilterDTO filter

    ) {
        User user = getUser(token);
        Role role = user.getRole();

        UUID profileId = switch (role) {
            case CUSTOMER -> getCustomerId(user.getId());
            case STAFF -> getFreelancerId(user.getId());
            default -> null;
        };

        return invoiceRepository.findAll(InvoiceSpecifications.withFiltersAndRole(filter, role, profileId), pageable)
                .map(i -> {
                    ContractSummaryDTO contractDto = contractMapper.toSummaryDto(i.getContract());
                    MilestoneResponseDTO milestoneDto = i.getMilestone() != null ? milestoneMapper.toDto(i.getMilestone()) : null;
                    return invoiceMapper.toSummaryDto(i, contractDto, milestoneDto);
                });
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

    @Override
    public InvoiceDetailDTO getInvoiceById(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found."));
        ContractDetailDTO contractDto = getContractDetailDTO(invoice.getContract());
        MilestoneResponseDTO milestoneDto = invoice.getMilestone() != null ? milestoneMapper.toDto(invoice.getMilestone()) : null;
        return invoiceMapper.toDetailDto(invoice, contractDto, milestoneDto);
    }

    @Override
    public InvoiceDetailDTO createInvoice(InvoiceRequestDTO request) {
        Contract contract = contractRepository.findById(request.getContractId()).orElseThrow(() ->
                new ResourceNotFoundException("Contract not found."));

        Invoice invoice = new Invoice();
        invoice.setContract(contract);
        invoice.setIssuedAt(OffsetDateTime.now(ZoneOffset.UTC));
        invoice.setDueDate(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));

        Milestone milestone = null;
        MilestoneResponseDTO milestoneDto = null;

        if (request.getMilestoneId() != null) {
            milestone = milestoneRepository.findById(request.getMilestoneId()).orElseThrow(() ->
                    new ResourceNotFoundException("Milestone not found."));
            if (!contract.getMilestones().contains(milestone)) {
                throw new IllegalArgumentException("Milestone does not belong to this contract.");
            }
            invoice.setMilestone(milestone);
            invoice.setAmount(milestone.getAmount());
            milestoneDto = milestoneMapper.toDto(milestone);
        } else {
            invoice.setAmount(contract.getAmount());
        }

        Invoice savedInvoice = invoiceRepository.save(invoice);
        if (milestone != null) {
            milestone.setInvoice(savedInvoice);
            milestoneRepository.save(milestone);
        }

        ContractDetailDTO contractDto = getContractDetailDTO(contract);

        return invoiceMapper.toDetailDto(savedInvoice, contractDto, milestoneDto);
    }

    @Override
    public InvoiceDetailDTO updateInvoice(UUID invoiceId, InvoiceRequestDTO request) {
        Invoice existentInvoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found."));
        if (request.getPayment() != null && existentInvoice.getPayment() != null) {
            throw new IllegalStateException("Cannot replace an existing payment.");
        }
        if (request.getPayment() == null && existentInvoice.getPayment() != null) {
            existentInvoice.setPayment(null);
        } else if (request.getPayment() != null) {
            existentInvoice.setPayment(request.getPayment());
        }
        Invoice updatedInvoice = invoiceRepository.save(existentInvoice);
        ContractDetailDTO contractDto = getContractDetailDTO(updatedInvoice.getContract());
        MilestoneResponseDTO milestoneDto = updatedInvoice.getMilestone() != null ? milestoneMapper.toDto(updatedInvoice.getMilestone()) : null;
        return invoiceMapper.toDetailDto(updatedInvoice, contractDto, milestoneDto);
    }

    @Override
    public InvoiceDetailDTO updateInvoiceStatusById(UUID invoiceId, InvoiceStatusRequestDTO request) {
        log.info("Updating status of invoice {} to {}", invoiceId, request.getStatus());
        Invoice existentInvoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found."));
        Contract contract = contractRepository.findById(existentInvoice.getContract().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found."));

        switch (request.getStatus()) {
            case PAID: {
                existentInvoice.setStatus(InvoiceStatus.PAID);
                if (existentInvoice.getMilestone() != null) {
                    MilestoneStatusRequestDTO milestoneStatusRequestDTO = MilestoneStatusRequestDTO.builder()
                            .status(MilestoneStatus.PAID)
                            .build();
                    milestoneService.updateMilestoneStatusById(existentInvoice.getMilestone().getId(), milestoneStatusRequestDTO);
                } else {
                    ContractStatusRequestDTO contractStatusRequestDTO = ContractStatusRequestDTO.builder()
                            .status(ContractStatus.COMPLETED)
                            .build();
                    contractService.updateContractStatusById(contract.getId(), contractStatusRequestDTO);
                }
                break;
            }
            case CANCELLED: {
                existentInvoice.setStatus(InvoiceStatus.CANCELLED);
                if (existentInvoice.getMilestone() != null) {
                    MilestoneStatusRequestDTO milestoneStatusRequestDTO = MilestoneStatusRequestDTO.builder()
                            .status(MilestoneStatus.PENDING)
                            .build();
                    milestoneService.updateMilestoneStatusById(existentInvoice.getMilestone().getId(), milestoneStatusRequestDTO);
                } else {
                    ContractStatusRequestDTO contractStatusRequestDTO = ContractStatusRequestDTO.builder()
                            .status(ContractStatus.ACTIVE)
                            .build();
                    contractService.updateContractStatusById(contract.getId(), contractStatusRequestDTO);
                }
                break;
            }
            case PENDING: {
                existentInvoice.setStatus(InvoiceStatus.PENDING);
                if (existentInvoice.getMilestone() != null) {
                    MilestoneStatusRequestDTO milestoneStatusRequestDTO = MilestoneStatusRequestDTO.builder()
                            .status(MilestoneStatus.PENDING)
                            .build();
                    milestoneService.updateMilestoneStatusById(existentInvoice.getMilestone().getId(), milestoneStatusRequestDTO);
                } else {
                    ContractStatusRequestDTO contractStatusRequestDTO = ContractStatusRequestDTO.builder()
                            .status(ContractStatus.ACTIVE)
                            .build();
                    contractService.updateContractStatusById(contract.getId(), contractStatusRequestDTO);
                }
                break;
            }
            default:
                log.info("No status change for invoice {}", invoiceId);
                break;
        }
        log.info("Invoice {} status updated to {}", invoiceId, request.getStatus());
        Invoice updatedInvoice = invoiceRepository.save(existentInvoice);
        log.info("Invoice {} saved with status {}", invoiceId, updatedInvoice.getStatus());
        ContractDetailDTO contractDto = getContractDetailDTO(updatedInvoice.getContract());
        log.info("Fetched contract details for invoice {}", invoiceId);
        MilestoneResponseDTO milestoneDto = updatedInvoice.getMilestone() != null ? milestoneMapper.toDto(updatedInvoice.getMilestone()) : null;
        log.info("Fetched milestone details for invoice {}", invoiceId);
        return invoiceMapper.toDetailDto(updatedInvoice, contractDto, milestoneDto);
    }

    @Override
    public void deleteInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found."));
        if (invoice.getPayment() != null) {
            throw new IllegalStateException("Cannot delete an invoice that has been paid.");
        }
        invoiceRepository.delete(invoice);

        if (invoice.getMilestone() != null) {
            MilestoneStatusRequestDTO milestoneStatusRequestDTO = MilestoneStatusRequestDTO.builder()
                    .status(MilestoneStatus.PENDING)
                    .build();
            milestoneService.updateMilestoneStatusById(invoice.getMilestone().getId(), milestoneStatusRequestDTO);
        } else {
            ContractStatusRequestDTO contractStatusRequest = ContractStatusRequestDTO.builder()
                    .status(ContractStatus.ACTIVE)
                    .build();
            contractService.updateContractStatusById(invoice.getContract().getId(), contractStatusRequest);
        }
    }

    private ContractDetailDTO getContractDetailDTO(Contract contract) {
        CustomerProfile customer = contract.getCustomer();
        FreelancerProfile freelancer = contract.getFreelancer();
        ContactDTO customerContact = ContactDTO.builder()
                .email(customer.getUser().getEmail())
                .phone(customer.getUser().getPhone())
                .address(addressMapper.toDto(customer.getUser().getAddress()))
                .build();
        ContactDTO freelancerContact = ContactDTO.builder()
                .email(freelancer.getUser().getEmail())
                .phone(freelancer.getUser().getPhone())
                .address(addressMapper.toDto(freelancer.getUser().getAddress()))
                .build();
        Set<InvoiceSummaryDTO> invoiceDtos = contract.getInvoices() != null ?
                contract.getInvoices().stream()
                        .map(i -> {
                            MilestoneResponseDTO milestoneDto = i.getMilestone() != null ? milestoneMapper.toDto(i.getMilestone()) : null;
                            return invoiceMapper.toSummaryDto(i, contractMapper.toSummaryDto(contract), milestoneDto);
                        })
                        .collect(Collectors.toSet())
                : Set.of();
        Set<MilestoneResponseDTO> milestoneDtos = contract.getMilestones() != null ?
                contract.getMilestones().stream()
                        .map(milestoneMapper::toDto)
                        .collect(Collectors.toSet())
                : Set.of();

        PaymentType paymentType = contract.getProject().getPaymentType();

        return contractMapper.toDetailDto(contract, customerContact, freelancerContact, invoiceDtos, milestoneDtos, paymentType);
    }
}
