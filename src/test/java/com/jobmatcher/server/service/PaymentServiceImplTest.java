package com.jobmatcher.server.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.ContractMapper;
import com.jobmatcher.server.mapper.InvoiceMapper;
import com.jobmatcher.server.mapper.MilestoneMapper;
import com.jobmatcher.server.mapper.PaymentMapper;
import com.jobmatcher.server.model.*;
import com.jobmatcher.server.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Mock private PaymentRepository paymentRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PaymentMapper paymentMapper;
    @Mock private ContractMapper contractMapper;
    @Mock private MilestoneMapper milestoneMapper;
    @Mock private InvoiceMapper invoiceMapper;
    @Mock private IInvoiceService invoiceService;
    @Mock private JwtService jwtService;
    @Mock private IUserService userService;
    @Mock private FreelancerProfileRepository freelancerProfileRepository;
    @Mock private CustomerProfileRepository customerProfileRepository;
    @Mock private ContractRepository contractRepository;
    @Mock private MilestoneRepository milestoneRepository;

    private User customerUser;
    private User staffUser;
    private PaymentFilterDTO paymentFilter;
    private Pageable unpaged;

    @BeforeEach
    void setUp() {
        customerUser = new User();
        customerUser.setId(UUID.randomUUID());
        customerUser.setRole(Role.CUSTOMER);

        staffUser = new User();
        staffUser.setId(UUID.randomUUID());
        staffUser.setRole(Role.STAFF);

        paymentFilter = new PaymentFilterDTO();
        unpaged = Pageable.unpaged();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllPayments_customerRole_returnsMappedPayments() {
        String token = "token";
        CustomerProfile customerProfile = new CustomerProfile();
        customerProfile.setId(UUID.randomUUID());

        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        Contract contract = new Contract();
        invoice.setContract(contract);
        Milestone milestone = new Milestone();
        invoice.setMilestone(milestone);

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setInvoice(invoice);

        Page<Payment> paymentPage = new PageImpl<>(List.of(payment));

        when(jwtService.extractUsername(token)).thenReturn("user@example.com");
        when(userService.getUserByEmail("user@example.com")).thenReturn(customerUser);
        when(customerProfileRepository.findByUserId(customerUser.getId())).thenReturn(Optional.of(customerProfile));
        when(paymentRepository.findAll(any(Specification.class), eq(unpaged))).thenReturn(paymentPage);
        when(contractMapper.toSummaryDto(contract)).thenReturn(ContractSummaryDTO.builder().id(UUID.randomUUID()).build());
        when(milestoneMapper.toDto(milestone)).thenReturn(MilestoneResponseDTO.builder().id(UUID.randomUUID()).build());
        when(invoiceMapper.toSummaryDto(any(), any(), any())).thenReturn(InvoiceSummaryDTO.builder().id(UUID.randomUUID()).build());
        when(paymentMapper.toSummaryDto(any(), any(), any(), any())).thenReturn(PaymentSummaryDTO.builder().id(UUID.randomUUID()).build());

        Page<PaymentSummaryDTO> result = paymentService.getAllPayments(token, unpaged, paymentFilter);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(paymentMapper).toSummaryDto(any(), any(), any(), any());
    }

    @Test
    void getAllPayments_staffRole_returnsMappedPayments() {
        FreelancerProfile freelancer = new FreelancerProfile();
        freelancer.setId(UUID.randomUUID());

        when(jwtService.extractUsername(anyString())).thenReturn("staff@example.com");
        when(userService.getUserByEmail(anyString())).thenReturn(staffUser);
        when(freelancerProfileRepository.findByUserId(staffUser.getId())).thenReturn(Optional.of(freelancer));

        Page<Payment> emptyPage = new PageImpl<>(Collections.emptyList());
        when(paymentRepository.findAll(ArgumentMatchers.<Specification<Payment>>any(), any(Pageable.class))).thenReturn(emptyPage);

        Page<PaymentSummaryDTO> result = paymentService.getAllPayments("token", unpaged, paymentFilter);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllPayments_customerProfileNotFound_throwsException() {
        when(jwtService.extractUsername(anyString())).thenReturn("user@example.com");
        when(userService.getUserByEmail(anyString())).thenReturn(customerUser);
        when(customerProfileRepository.findByUserId(customerUser.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.getAllPayments("token", unpaged, paymentFilter));
    }

    @Test
    void getAllPayments_freelancerProfileNotFound_throwsException() {
        when(jwtService.extractUsername(anyString())).thenReturn("staff@example.com");
        when(userService.getUserByEmail(anyString())).thenReturn(staffUser);
        when(freelancerProfileRepository.findByUserId(staffUser.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.getAllPayments("token", unpaged, paymentFilter));
    }

    @Test
    void getPaymentById_found_returnsDto() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment();
        Invoice invoice = new Invoice();
        Contract contract = new Contract();
        Milestone milestone = new Milestone();
        invoice.setContract(contract);
        invoice.setMilestone(milestone);
        payment.setInvoice(invoice);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(contractMapper.toSummaryDto(contract)).thenReturn(ContractSummaryDTO.builder().id(UUID.randomUUID()).build());
        when(milestoneMapper.toDto(milestone)).thenReturn(MilestoneResponseDTO.builder().id(UUID.randomUUID()).build());
        when(invoiceMapper.toSummaryDto(any(), any(), any())).thenReturn(InvoiceSummaryDTO.builder().id(UUID.randomUUID()).build());
        when(paymentMapper.toDetailDto(any(), any(), any(), any())).thenReturn(PaymentDetailDTO.builder().id(UUID.randomUUID()).build());

        PaymentDetailDTO result = paymentService.getPaymentById(paymentId);
        assertNotNull(result);
    }

    @Test
    void getPaymentById_notFound_throwsException() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> paymentService.getPaymentById(paymentId));
    }

    @Test
    void getPaymentByInvoiceId_found_returnsDto() {
        UUID invoiceId = UUID.randomUUID();
        Payment payment = new Payment();
        Invoice invoice = new Invoice();
        Contract contract = new Contract();
        Milestone milestone = new Milestone();
        invoice.setContract(contract);
        invoice.setMilestone(milestone);
        payment.setInvoice(invoice);

        when(paymentRepository.findByInvoiceId(invoiceId)).thenReturn(Optional.of(payment));
        when(contractMapper.toSummaryDto(contract)).thenReturn(ContractSummaryDTO.builder().id(UUID.randomUUID()).build());
        when(milestoneMapper.toDto(milestone)).thenReturn(MilestoneResponseDTO.builder().id(UUID.randomUUID()).build());
        when(invoiceMapper.toSummaryDto(any(), any(), any())).thenReturn(InvoiceSummaryDTO.builder().id(UUID.randomUUID()).build());
        when(paymentMapper.toDetailDto(any(), any(), any(), any())).thenReturn(PaymentDetailDTO.builder().id(UUID.randomUUID()).build());

        PaymentDetailDTO result = paymentService.getPaymentByInvoiceId(invoiceId);
        assertNotNull(result);
    }

    @Test
    void getPaymentByInvoiceId_notFound_throwsException() {
        UUID invoiceId = UUID.randomUUID();
        when(paymentRepository.findByInvoiceId(invoiceId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> paymentService.getPaymentByInvoiceId(invoiceId));
    }

    @Test
    void createPayment_success() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setAmount(BigDecimal.valueOf(100));
        Contract contract = new Contract();
        invoice.setContract(contract);

        PaymentRequestDTO request = PaymentRequestDTO.builder().invoiceId(invoiceId.toString()).build();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentRepository.existsByInvoiceId(invoiceId)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Payment result = paymentService.createPayment(request);

        assertNotNull(result);
        assertEquals(invoice, result.getInvoice());
    }

    @Test
    void createPayment_invoiceNotFound_throwsException() {
        UUID invoiceId = UUID.randomUUID();
        PaymentRequestDTO request = PaymentRequestDTO.builder().invoiceId(invoiceId.toString()).build();
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.createPayment(request));
    }

    @Test
    void createPayment_invoiceAlreadyPaid_throwsException() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.PAID);

        PaymentRequestDTO request = PaymentRequestDTO.builder().invoiceId(invoiceId.toString()).build();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        assertThrows(IllegalStateException.class, () -> paymentService.createPayment(request));
    }

    @Test
    void createPayment_paymentAlreadyExists_throwsException() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setStatus(InvoiceStatus.PENDING);

        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .invoiceId(invoiceId.toString())
                .build();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        lenient().when(paymentRepository.existsByInvoiceId(any(UUID.class))).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> paymentService.createPayment(request));
    }

    @Test
    void markInvoicePaid_happyPath() {
        UUID invoiceId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        UUID milestoneId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        Contract contract = new Contract();
        contract.setId(contractId);
        contract.setAmount(BigDecimal.valueOf(500));
        contract.setTotalPaid(BigDecimal.ZERO);
        contract.setRemainingBalance(BigDecimal.valueOf(500));
        contract.setMilestones(new HashSet<>());
        contract.setInvoices(new HashSet<>());

        Milestone milestone = new Milestone();
        milestone.setId(milestoneId);
        milestone.setContract(contract);
        milestone.setAmount(BigDecimal.valueOf(500));
        milestone.setEstimatedDuration(5);
        milestone.setPlannedStartDate(LocalDate.now());
        milestone.setStatus(MilestoneStatus.PENDING);

        contract.getMilestones().add(milestone);

        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setContract(contract);
        invoice.setMilestone(milestone);
        invoice.setAmount(BigDecimal.valueOf(500));
        invoice.setStatus(InvoiceStatus.PENDING);

        contract.getInvoices().add(invoice);
        milestone.setInvoice(invoice);

        Payment payment = new Payment();
        payment.setId(paymentId);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentRepository.existsByInvoiceId(invoiceId)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setId(paymentId);
            return p;
        });

        when(milestoneRepository.save(any(Milestone.class))).thenAnswer(i -> i.getArgument(0));
        when(contractRepository.save(any(Contract.class))).thenAnswer(i -> i.getArgument(0));

        when(invoiceService.updateInvoice(eq(invoiceId), any(InvoiceRequestDTO.class)))
                .thenReturn(InvoiceDetailDTO.builder().id(invoice.getId()).build());

        when(invoiceService.updateInvoiceStatusById(eq(invoiceId), any(InvoiceStatusRequestDTO.class)))
                .thenAnswer(invocation -> {
                    InvoiceStatusRequestDTO dto = invocation.getArgument(1);
                    invoice.setStatus(dto.getStatus());
                    return InvoiceDetailDTO.builder().id(invoice.getId()).build();
                });

        paymentService.markInvoicePaid(invoiceId);

        assertNotNull(invoice.getMilestone().getPayment(), "Milestone should have payment");
        assertNull(invoice.getContract().getPayment(), "Contract should NOT have payment when milestones exist");
        assertEquals(InvoiceStatus.PAID, invoice.getStatus(), "Invoice should be marked as PAID");
        assertEquals(BigDecimal.valueOf(500), contract.getTotalPaid(), "Contract totalPaid should update");
        assertEquals(BigDecimal.ZERO, contract.getRemainingBalance(), "Contract remainingBalance should update");
    }

    @Test
    void getAllPayments_nullUserIdForFreelancer_throwsIllegalArgumentException() {
        String token = "dummy-token";
        User user = new User();
        user.setRole(Role.STAFF);
        user.setId(null);

        when(jwtService.extractUsername(token)).thenReturn("test@example.com");
        when(userService.getUserByEmail("test@example.com")).thenReturn(user);

        assertThrows(IllegalArgumentException.class, () ->
                paymentService.getAllPayments(token, Pageable.unpaged(), new PaymentFilterDTO()));
    }

    @Test
    void getAllPayments_nullUserIdForCustomer_throwsIllegalArgumentException() {
        String token = "dummy-token";
        User user = new User();
        user.setRole(Role.CUSTOMER);
        user.setId(null);

        when(jwtService.extractUsername(token)).thenReturn("test@example.com");
        when(userService.getUserByEmail("test@example.com")).thenReturn(user);

        assertThrows(IllegalArgumentException.class, () ->
                paymentService.getAllPayments(token, Pageable.unpaged(), new PaymentFilterDTO()));
    }

    @Test
    void markInvoicePaid_invoiceNotFound_throwsResourceNotFoundException() {
        UUID invoiceId = UUID.randomUUID();
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.markInvoicePaid(invoiceId));
    }

    @Test
    void markInvoicePaid_invoiceWithoutContract_throwsResourceNotFoundException() {
        UUID invoiceId = UUID.randomUUID();

        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setAmount(BigDecimal.valueOf(100));
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setContract(null);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentRepository.existsByInvoiceId(invoiceId)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        assertThrows(ResourceNotFoundException.class, () -> paymentService.markInvoicePaid(invoiceId));
    }

    @Test
    void markInvoicePaid_invoiceWithoutMilestone_updatesContract() {
        UUID invoiceId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();

        Contract contract = new Contract();
        contract.setId(contractId);
        contract.setAmount(BigDecimal.valueOf(100));
        contract.setTotalPaid(BigDecimal.ZERO);
        contract.setMilestones(new HashSet<>());

        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setContract(contract);
        invoice.setAmount(BigDecimal.valueOf(100));
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setMilestone(null);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentRepository.existsByInvoiceId(invoiceId)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(contractRepository.save(any(Contract.class))).thenAnswer(i -> i.getArgument(0));

        paymentService.markInvoicePaid(invoiceId);

        assertEquals(BigDecimal.valueOf(100), contract.getTotalPaid());
        assertEquals(BigDecimal.ZERO, contract.getRemainingBalance());
    }

    @Test
    void markInvoicePaid_noMilestones_setsPaymentOnContract() {
        UUID invoiceId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        Contract contract = new Contract();
        contract.setId(contractId);
        contract.setAmount(BigDecimal.valueOf(200));
        contract.setTotalPaid(BigDecimal.ZERO);
        contract.setRemainingBalance(BigDecimal.valueOf(200));
        contract.setMilestones(new HashSet<>());

        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setContract(contract);
        invoice.setAmount(BigDecimal.valueOf(200));
        invoice.setStatus(InvoiceStatus.PENDING);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentRepository.existsByInvoiceId(invoiceId)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setId(paymentId);
            return p;
        });

        when(invoiceService.updateInvoice(eq(invoiceId), any())).thenReturn(null);
        when(invoiceService.updateInvoiceStatusById(eq(invoiceId), any())).thenAnswer(invocation -> {
            InvoiceStatusRequestDTO dto = invocation.getArgument(1);
            invoice.setStatus(dto.getStatus());
            return null;
        });

        paymentService.markInvoicePaid(invoiceId);

        assertNotNull(contract.getPayment(), "Contract should have payment if no milestones");
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertEquals(BigDecimal.valueOf(200), contract.getTotalPaid());
        assertEquals(BigDecimal.ZERO, contract.getRemainingBalance());
    }

    @Test
    void markInvoicePaid_nullMilestone_throwsResourceNotFoundException() {
        UUID invoiceId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();

        Contract contract = new Contract();
        contract.setId(contractId);
        contract.setAmount(BigDecimal.valueOf(100));
        contract.setTotalPaid(BigDecimal.ZERO);
        contract.setMilestones(new HashSet<>(Set.of(new Milestone())));

        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setContract(contract);
        invoice.setAmount(BigDecimal.valueOf(100));
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setMilestone(null);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentRepository.existsByInvoiceId(invoiceId)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                paymentService.markInvoicePaid(invoiceId)
        );

        assertEquals("Milestone not found for invoice: " + invoiceId, exception.getMessage());
    }

}
