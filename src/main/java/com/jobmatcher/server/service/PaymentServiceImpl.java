package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.ContractMapper;
import com.jobmatcher.server.mapper.InvoiceMapper;
import com.jobmatcher.server.mapper.MilestoneMapper;
import com.jobmatcher.server.mapper.PaymentMapper;
import com.jobmatcher.server.model.*;
import com.jobmatcher.server.repository.*;
import com.jobmatcher.server.specification.PaymentSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
public class PaymentServiceImpl implements IPaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentMapper paymentMapper;
    private final ContractMapper contractMapper;
    private final MilestoneMapper mileStoneMapper;
    private final InvoiceMapper invoiceMapper;
    private final IInvoiceService invoiceService;
    private final JwtService jwtService;
    private final IUserService userService;
    private final FreelancerProfileRepository freelancerProfileRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final ContractRepository contractRepository;
    private final MilestoneRepository milestoneRepository;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            InvoiceRepository invoiceRepository,
            PaymentMapper paymentMapper,
            ContractMapper contractMapper,
            MilestoneMapper mileStoneMapper,
            InvoiceMapper invoiceMapper,
            IInvoiceService invoiceService,
            JwtService jwtService,
            IUserService userService,
            FreelancerProfileRepository freelancerProfileRepository,
            CustomerProfileRepository customerProfileRepository,
            ContractRepository contractRepository, MilestoneRepository milestoneRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentMapper = paymentMapper;
        this.contractMapper = contractMapper;
        this.mileStoneMapper = mileStoneMapper;
        this.invoiceMapper = invoiceMapper;
        this.invoiceService = invoiceService;
        this.jwtService = jwtService;
        this.userService = userService;
        this.freelancerProfileRepository = freelancerProfileRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.contractRepository = contractRepository;
        this.milestoneRepository = milestoneRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<PaymentSummaryDTO> getAllPayments(
            String token,
            Pageable pageable,
            PaymentFilterDTO filter
    ) {
        User user = getUser(token);
        Role role = user.getRole();

        UUID profileId = switch (role) {
            case CUSTOMER -> getCustomerId(user.getId());
            case STAFF -> getFreelancerId(user.getId());
            default -> null;
        };

        return paymentRepository.findAll(PaymentSpecification.withFiltersAndRole(filter, role, profileId), pageable)
                .map(payment -> {
                    PaymentDetail paymentDetail = getPaymentDetail(payment.getInvoice());
                    return paymentMapper.toSummaryDto(
                            payment,
                            paymentDetail.contractSummaryDTO(),
                            paymentDetail.milestoneResponseDTO(),
                            paymentDetail.invoiceSummaryDTO()
                    );
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
    public PaymentDetailDTO getPaymentById(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        PaymentDetail paymentDetail = getPaymentDetail(payment.getInvoice());

        return paymentMapper.toDetailDto(payment, paymentDetail.contractSummaryDTO, paymentDetail.milestoneResponseDTO, paymentDetail.invoiceSummaryDTO);
    }

    @Transactional(readOnly = true)
    @Override
    public PaymentDetailDTO getPaymentByInvoiceId(UUID invoiceId) {
        Payment payment = paymentRepository.findByInvoiceId(invoiceId).orElseThrow(() ->
                new ResourceNotFoundException("Payment not found"));
        PaymentDetail paymentDetail = getPaymentDetail(payment.getInvoice());
        return paymentMapper.toDetailDto(payment, paymentDetail.contractSummaryDTO, paymentDetail.milestoneResponseDTO, paymentDetail.invoiceSummaryDTO);
    }

    @Override
    public Payment createPayment(PaymentRequestDTO request) {
        Invoice invoice = invoiceRepository.findById(UUID.fromString(request.getInvoiceId())).orElseThrow(() ->
                new ResourceNotFoundException("Invoice not found"));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Invoice is already marked as PAID");
        }
        if (paymentRepository.existsByInvoiceId(invoice.getId())) {
            throw new IllegalStateException("Payment already exists for this invoice");
        }
        Payment payment = new Payment();
        payment.setContract(invoice.getContract());
        payment.setMilestone(invoice.getMilestone());
        payment.setInvoice(invoice);
        payment.setAmount(invoice.getAmount());
        payment.setPaidAt(OffsetDateTime.now(ZoneOffset.UTC));

        return paymentRepository.save(payment);
    }

    @Override
    public void markInvoicePaid(UUID invoiceId) {
        log.info("Marking invoice {} as PAID", invoiceId);
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow(() ->
                new ResourceNotFoundException("Invoice not found"));

        PaymentRequestDTO paymentRequestDto = PaymentRequestDTO.builder()
                .invoiceId(invoiceId.toString())
                .build();
        Payment payment = createPayment(paymentRequestDto);
        log.info("Created payment {} for invoice {}", payment.getId(), invoiceId);

        InvoiceRequestDTO invoiceRequestDTO = InvoiceRequestDTO.builder()
                .payment(payment)
                .build();
        InvoiceStatusRequestDTO invoiceStatusRequestDTO = InvoiceStatusRequestDTO.builder()
                .status(InvoiceStatus.PAID)
                .build();
        log.info("Updating invoice {} status to PAID", invoiceId);
        invoiceService.updateInvoice(invoiceId, invoiceRequestDTO);
        invoiceService.updateInvoiceStatusById(invoiceId, invoiceStatusRequestDTO);
        log.info("Invoice {} marked as PAID", invoiceId);

        Contract contract = invoice.getContract();
        if(contract == null) {
            log.warn("Invoice {} has no associated contract, skipping contract update", invoiceId);
            throw new ResourceNotFoundException("Contract not found for invoice: " + invoiceId);
        }
        if(contract.getMilestones().isEmpty()) {
            updateContract(contract, payment, invoice);
        } else {
            updateMilestone(invoiceId, invoice, payment);
            updateContract(contract, payment, invoice);
        }
    }

    private void updateMilestone(UUID invoiceId, Invoice invoice, Payment payment) {
        Milestone milestone = invoice.getMilestone();
        if (milestone == null) {
            log.warn("Invoice {} has no associated milestone, skipping contract payment update", invoiceId);
            throw new ResourceNotFoundException("Milestone not found for invoice: " + invoiceId);
        }
        milestone.setPayment(payment);
        milestone.setActualEndDate(LocalDate.now(ZoneOffset.UTC));

        milestoneRepository.save(milestone);
        log.info("Milestone {} marked as paid", milestone.getId());
    }

    private void updateContract(Contract contract, Payment payment, Invoice invoice) {
        if(contract.getMilestones().isEmpty()) {
            contract.setPayment(payment);
        }
        contract.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));

        BigDecimal newTotalPaid = contract.getTotalPaid().add(invoice.getAmount());
        contract.setTotalPaid(newTotalPaid);
        contract.setRemainingBalance(contract.getAmount().subtract(newTotalPaid));

        contractRepository.save(contract);
        log.info("Contract {} marked as completed", contract.getId());
    }

    private record PaymentDetail(ContractSummaryDTO contractSummaryDTO, MilestoneResponseDTO milestoneResponseDTO,
                                 InvoiceSummaryDTO invoiceSummaryDTO) {
    }

    private PaymentDetail getPaymentDetail(Invoice invoice) {
        Contract contract = invoice.getContract();
        ContractSummaryDTO contractSummaryDTO = contractMapper.toSummaryDto(contract);
        Milestone milestone = invoice.getMilestone();
        MilestoneResponseDTO milestoneResponseDTO = milestone != null ? mileStoneMapper.toDto(milestone) : null;
        InvoiceSummaryDTO invoiceSummaryDTO = invoiceMapper.toSummaryDto(invoice, contractSummaryDTO, milestoneResponseDTO);
        return new PaymentDetail(contractSummaryDTO, milestoneResponseDTO, invoiceSummaryDTO);
    }
}
