package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.AddressMapper;
import com.jobmatcher.server.mapper.ContractMapper;
import com.jobmatcher.server.mapper.InvoiceMapper;
import com.jobmatcher.server.mapper.MilestoneMapper;
import com.jobmatcher.server.model.*;
import com.jobmatcher.server.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Transactional(rollbackFor = Exception.class)
@Service
public class InvoiceServiceImpl implements IInvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final ContractRepository contractRepository;
    private final MilestoneRepository milestoneRepository;
    private final ContractMapper contractMapper;
    private final MilestoneMapper milestoneMapper;
    private final AddressMapper addressMapper;
    private final IContractService contractService;
    private final IMilestoneService milestoneService;

    public InvoiceServiceImpl(
            InvoiceRepository invoiceRepository,
            InvoiceMapper invoiceMapper,
            ContractRepository contractRepository,
            MilestoneRepository milestoneRepository,
            ContractMapper contractMapper,
            MilestoneMapper milestoneMapper,
            AddressMapper addressMapper, IContractService contractService, IMilestoneService milestoneService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceMapper = invoiceMapper;
        this.contractRepository = contractRepository;
        this.milestoneRepository = milestoneRepository;
        this.contractMapper = contractMapper;
        this.milestoneMapper = milestoneMapper;
        this.addressMapper = addressMapper;
        this.contractService = contractService;
        this.milestoneService = milestoneService;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<InvoiceSummaryDTO> getAllInvoicesByCustomerId(UUID customerId, Pageable pageable) {
        Page<Invoice> invoices = invoiceRepository.findAllByCustomerId(customerId, pageable);

        return invoices.map(invoice -> {
            ContractSummaryDTO contractDto = contractMapper.toSummaryDto(invoice.getContract());
            MilestoneResponseDTO milestoneDto = invoice.getMilestone() != null ? milestoneMapper.toDto(invoice.getMilestone()) : null;
            return invoiceMapper.toSummaryDto(invoice, contractDto, milestoneDto);
        });
    }

    @Transactional(readOnly = true)
    @Override
    public Page<InvoiceSummaryDTO> getAllInvoicesByFreelancerId(UUID freelancerId, Pageable pageable) {
        Page<Invoice> invoices = invoiceRepository.findAllByFreelancerId(freelancerId, pageable);

        return invoices.map(invoice -> {
            ContractSummaryDTO contractDto = contractMapper.toSummaryDto(invoice.getContract());
            MilestoneResponseDTO milestoneDto = invoice.getMilestone() != null ? milestoneMapper.toDto(invoice.getMilestone()) : null;
            return invoiceMapper.toSummaryDto(invoice, contractDto, milestoneDto);
        });
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
        if (request.getContractId() == null) {
            throw new IllegalArgumentException("ContractId must be provided.");
        }
        Milestone milestone = null;
        MilestoneResponseDTO milestoneDto = null;
        if (request.getMilestoneId() != null) {
            milestone = milestoneRepository.findById(request.getMilestoneId()).orElseThrow(() ->
                    new ResourceNotFoundException("Milestone not found."));
        }
        Contract contract = contractRepository.findById(request.getContractId()).orElseThrow(() ->
                new ResourceNotFoundException("Contract not found."));

        if (milestone != null && !contract.getMilestones().contains(milestone)) {
            throw new IllegalArgumentException("Milestone does not belong to the specified contract");
        }

        Invoice invoice = new Invoice();
        invoice.setContract(contract);
        if (milestone != null) {
            invoice.setMilestone(milestone);
            invoice.setAmount(milestone.getAmount());
            milestoneDto = milestoneMapper.toDto(milestone);
        } else {
            invoice.setAmount(contract.getAmount());
        }
        invoice.setIssuedAt(OffsetDateTime.now(ZoneOffset.UTC));
        invoice.setDueDate(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));

        Invoice savedInvoice = invoiceRepository.save(invoice);

        ContractDetailDTO contractDto = getContractDetailDTO(contract);

        return invoiceMapper.toDetailDto(savedInvoice, contractDto, milestoneDto);
    }

    @Override
    public InvoiceDetailDTO updateInvoice(UUID invoiceId, InvoiceRequestDTO request) {
        Invoice existentInvoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found."));
        // Update status if provided
        if (request.getStatus() != null) {
            ContractRequestDTO contractRequestDTO;
            existentInvoice.setStatus(request.getStatus());
            if (request.getStatus() == InvoiceStatus.PAID) {
                if (existentInvoice.getMilestone() == null) {
                    contractRequestDTO = ContractRequestDTO.builder()
                            .status(ContractStatus.COMPLETED)
                            .paymentStatus(PaymentStatus.PAID)
                            .build();
                } else {
                    contractRequestDTO = ContractRequestDTO.builder()
                            .status(ContractStatus.ACTIVE)
                            .paymentStatus(PaymentStatus.PARTIALLY_PAID)
                            .build();
                    MilestoneRequestDTO milestoneRequestDTO = MilestoneRequestDTO.builder()
                            .status(MilestoneStatus.COMPLETED)
                            .paymentStatus(PaymentStatus.PAID)
                            .build();
                    milestoneService.updateMilestone(existentInvoice.getMilestone().getId(), milestoneRequestDTO);
                }
                contractService.updateContract(request.getContractId(), contractRequestDTO);
            } else if (request.getStatus() == InvoiceStatus.PENDING) {
                if (existentInvoice.getMilestone() == null) {
                    contractRequestDTO = ContractRequestDTO.builder()
                            .status(ContractStatus.ACTIVE)
                            .paymentStatus(PaymentStatus.PENDING)
                            .build();
                } else {
                    MilestoneRequestDTO milestoneRequestDTO = MilestoneRequestDTO.builder()
                            .status(MilestoneStatus.PENDING)
                            .paymentStatus(PaymentStatus.PENDING)
                            .build();
                    milestoneService.updateMilestone(existentInvoice.getMilestone().getId(), milestoneRequestDTO);
                    boolean anyMilestonePaid = isAnyMilestonePaid(existentInvoice);
                    contractRequestDTO = ContractRequestDTO.builder()
                            .status(ContractStatus.ACTIVE)
                            .paymentStatus(anyMilestonePaid ? PaymentStatus.PARTIALLY_PAID : PaymentStatus.PENDING)
                            .build();
                }
                contractService.updateContract(request.getContractId(), contractRequestDTO);
            } else if (request.getStatus() == InvoiceStatus.CANCELLED) {
                if (existentInvoice.getMilestone() == null) {
                    contractRequestDTO = ContractRequestDTO.builder()
                            .status(ContractStatus.ACTIVE)
                            .paymentStatus(PaymentStatus.PENDING)
                            .build();
                    contractService.updateContract(request.getContractId(), contractRequestDTO);
                } else {
                    MilestoneRequestDTO milestoneRequestDTO = MilestoneRequestDTO.builder()
                            .status(MilestoneStatus.PENDING)
                            .paymentStatus(PaymentStatus.PENDING)
                            .build();
                    milestoneService.updateMilestone(existentInvoice.getMilestone().getId(), milestoneRequestDTO);
                    boolean anyMilestonePaid = isAnyMilestonePaid(existentInvoice);
                    contractRequestDTO = ContractRequestDTO.builder()
                            .status(ContractStatus.ACTIVE)
                            .paymentStatus(anyMilestonePaid ? PaymentStatus.PARTIALLY_PAID : PaymentStatus.PENDING)
                            .build();
                }
                contractService.updateContract(request.getContractId(), contractRequestDTO);
            }
        }
        // Update payment if provided
        // Only allow setting or clearing the payment, not replacing an existing one
        if (request.getPayment() != null && existentInvoice.getPayment() != null) {
            throw new IllegalStateException("Cannot replace an existing payment.");
        }
        if (request.getPayment() == null && existentInvoice.getPayment() != null) {
            // clearing the payment from deletePayment
            existentInvoice.setPayment(null);
        } else if (request.getPayment() != null) {
            // setting payment from createPayment
            existentInvoice.setPayment(request.getPayment());
        }
        Invoice updatedInvoice = invoiceRepository.save(existentInvoice);
        ContractDetailDTO contractDto = getContractDetailDTO(updatedInvoice.getContract());
        MilestoneResponseDTO milestoneDto = updatedInvoice.getMilestone() != null ? milestoneMapper.toDto(updatedInvoice.getMilestone()) : null;
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

        // Update related milestone and contract statuses
        ContractRequestDTO contractRequestDTO;
        if(invoice.getMilestone() != null){
            MilestoneRequestDTO milestoneRequestDTO = MilestoneRequestDTO.builder()
                    .status(MilestoneStatus.PENDING)
                    .paymentStatus(PaymentStatus.PENDING)
                    .build();
            milestoneService.updateMilestone(invoice.getMilestone().getId(), milestoneRequestDTO);
            boolean anyMilestonePaid = isAnyMilestonePaid(invoice);
            contractRequestDTO = ContractRequestDTO.builder()
                    .status(ContractStatus.ACTIVE)
                    .paymentStatus(anyMilestonePaid ? PaymentStatus.PARTIALLY_PAID : PaymentStatus.PENDING)
                    .build();
        } else {
            contractRequestDTO = ContractRequestDTO.builder()
                    .status(ContractStatus.ACTIVE)
                    .paymentStatus(PaymentStatus.PENDING)
                    .build();
        }
        contractService.updateContract(invoice.getContract().getId(), contractRequestDTO);
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
        Set<MilestoneResponseDTO> milestoneDtos = contract.getMilestones() != null ?
                contract.getMilestones().stream()
                        .map(milestoneMapper::toDto)
                        .collect(Collectors.toSet())
                : Set.of();

        ContractDetailDTO contractDto = contractMapper.toDetailDto(contract, customerContact, freelancerContact, milestoneDtos);
        return contractDto;
    }

    private static boolean isAnyMilestonePaid(Invoice existentInvoice) {
        boolean anyMilestonePaid = false;
        Set<Milestone> milestones = existentInvoice.getContract().getMilestones();
        for (Milestone milestone : milestones) {
            if (milestone.getPaymentStatus() == PaymentStatus.PAID) {
                anyMilestonePaid = true;
                break;
            }
        }
        return anyMilestonePaid;
    }

}
