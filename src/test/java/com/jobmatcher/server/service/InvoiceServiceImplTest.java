package com.jobmatcher.server.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.*;
import com.jobmatcher.server.model.*;
import com.jobmatcher.server.repository.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private InvoiceMapper invoiceMapper;
    @Mock
    private ContractMapper contractMapper;
    @Mock
    private MilestoneMapper milestoneMapper;
    @Mock
    private JwtService jwtService;
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private MilestoneRepository milestoneRepository;
    @Mock
    private IContractService contractService;
    @Mock
    private IMilestoneService milestoneService;
    @Mock
    private IUserService userService;
    @Mock
    private FreelancerProfileRepository freelancerProfileRepository;
    @Mock
    private CustomerProfileRepository customerProfileRepository;
    @Mock
    private AddressMapper addressMapper;


    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    Invoice invoice;
    Milestone milestone;
    Contract contract;
    User customerUser;
    User freelancerUser;

    UUID invoiceId;
    UUID contractId;
    UUID milestoneId;
    UUID customerUserId;
    UUID freelancerUserId;

    @BeforeEach
    void setUp() {
        invoiceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        contractId = UUID.randomUUID();
        milestoneId = UUID.randomUUID();
        customerUserId = UUID.randomUUID();
        freelancerUserId = UUID.randomUUID();

        // Users
        customerUser = new User();
        customerUser.setId(customerUserId);
        customerUser.setEmail("customer@test.com");
        customerUser.setPhone("12345");
        customerUser.setAddress(new Address());
        customerUser.setRole(Role.CUSTOMER);

        freelancerUser = new User();
        freelancerUser.setId(freelancerUserId);
        freelancerUser.setEmail("freelancer@test.com");
        freelancerUser.setPhone("67890");
        freelancerUser.setAddress(new Address());
        freelancerUser.setRole(Role.STAFF);

        // Profiles
        CustomerProfile customer = new CustomerProfile();
        customer.setUser(customerUser);

        FreelancerProfile freelancer = new FreelancerProfile();
        freelancer.setUser(freelancerUser);

        Project project = new Project();
        project.setId(projectId);
        project.setFreelancer(freelancer);
        project.setCustomer(customer);
        project.setStatus(ProjectStatus.IN_PROGRESS);
        project.setContract(contract);
        project.setBudget(BigDecimal.valueOf(100));
        project.setDeadline(LocalDate.now().plusDays(30));
        project.setTitle("Test Project");
        project.setDescription("Test Project");
        project.setPaymentType(PaymentType.UPON_COMPLETION);

        contract = new Contract();
        contract.setId(contractId);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setAmount(BigDecimal.valueOf(100));
        contract.setMilestones(new HashSet<>());
        contract.setInvoices(new HashSet<>());
        contract.setCustomer(customer);
        contract.setFreelancer(freelancer);
        contract.setProject(project);
        contract.setInvoices(new HashSet<>());

        milestone = new Milestone();
        milestone.setId(milestoneId);
        milestone.setAmount(BigDecimal.valueOf(50));
        milestone.setContract(contract);
        contract.getMilestones().add(milestone);

        invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setContract(contract);
        invoice.setAmount(BigDecimal.valueOf(50));
        invoice.setMilestone(milestone);
        invoice.setStatus(InvoiceStatus.PENDING);
    }

    @Test
    void getAllInvoices_customerRole_shouldReturnMappedPage() {
        String token = "jwt.token";
        Pageable pageable = PageRequest.of(0, 10);
        InvoiceFilterDTO filter = InvoiceFilterDTO.builder().build();

        User user = new User();
        user.setId(customerUserId);
        user.setRole(Role.CUSTOMER);

        CustomerProfile profile = new CustomerProfile();
        profile.setId(UUID.randomUUID());

        Invoice invoice1 = new Invoice();
        invoice1.setId(UUID.randomUUID());
        Invoice invoice2 = new Invoice();
        invoice2.setId(UUID.randomUUID());

        Page<Invoice> entityPage = new PageImpl<>(List.of(invoice1, invoice2));

        when(jwtService.extractUsername(token)).thenReturn("customer@test.com");
        when(userService.getUserByEmail("customer@test.com")).thenReturn(user);
        when(customerProfileRepository.findByUserId(customerUserId)).thenReturn(Optional.of(profile));
        when(invoiceRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(entityPage);

        when(contractMapper.toSummaryDto(any())).thenReturn(ContractSummaryDTO.builder().id(UUID.randomUUID()).build());
        when(invoiceMapper.toSummaryDto(any(), any(), any()))
                .thenReturn(InvoiceSummaryDTO.builder().id(UUID.randomUUID()).build());

        Page<InvoiceSummaryDTO> result = invoiceService.getAllInvoices(token, pageable, filter);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        verify(customerProfileRepository).findByUserId(customerUserId);
        verify(invoiceRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getAllInvoices_staffRole_shouldReturnMappedPage() {
        String token = "jwt.token";
        Pageable pageable = PageRequest.of(0, 10);
        InvoiceFilterDTO filter = InvoiceFilterDTO.builder().build();

        User user = new User();
        user.setId(freelancerUserId);
        user.setRole(Role.STAFF);

        FreelancerProfile profile = new FreelancerProfile();
        profile.setId(UUID.randomUUID());

        Page<Invoice> entityPage = new PageImpl<>(List.of(invoice));

        when(jwtService.extractUsername(token)).thenReturn("freelancer@test.com");
        when(userService.getUserByEmail("freelancer@test.com")).thenReturn(user);
        when(freelancerProfileRepository.findByUserId(freelancerUserId)).thenReturn(Optional.of(profile));
        when(invoiceRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(entityPage);

        when(contractMapper.toSummaryDto(any())).thenReturn(ContractSummaryDTO.builder().id(UUID.randomUUID()).build());
        when(invoiceMapper.toSummaryDto(any(), any(), any()))
                .thenReturn(InvoiceSummaryDTO.builder().id(UUID.randomUUID()).build());

        Page<InvoiceSummaryDTO> result = invoiceService.getAllInvoices(token, pageable, filter);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(freelancerProfileRepository).findByUserId(freelancerUserId);
        verify(invoiceRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getAllInvoices_otherRole_shouldUseNullProfileId() {
        String token = "jwt.token";
        Pageable pageable = PageRequest.of(0, 10);
        InvoiceFilterDTO filter = InvoiceFilterDTO.builder().build();

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(Role.ADMIN); // not CUSTOMER or STAFF

        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        Page<Invoice> entityPage = new PageImpl<>(List.of(invoice));

        when(jwtService.extractUsername(token)).thenReturn("admin@test.com");
        when(userService.getUserByEmail("admin@test.com")).thenReturn(user);
        when(invoiceRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(entityPage);

        when(contractMapper.toSummaryDto(any())).thenReturn(ContractSummaryDTO.builder().id(UUID.randomUUID()).build());
        when(invoiceMapper.toSummaryDto(any(), any(), any()))
                .thenReturn(InvoiceSummaryDTO.builder().id(UUID.randomUUID()).build());

        Page<InvoiceSummaryDTO> result = invoiceService.getAllInvoices(token, pageable, filter);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(invoiceRepository).findAll(any(Specification.class), eq(pageable));
        verifyNoInteractions(customerProfileRepository, freelancerProfileRepository);
    }

    @Test
    void getAllInvoices_customerProfileNotFound_shouldThrow() {
        String token = "jwt.token";
        Pageable pageable = PageRequest.of(0, 10);
        InvoiceFilterDTO filter = InvoiceFilterDTO.builder().build();

        User user = new User();
        user.setId(customerUserId);
        user.setRole(Role.CUSTOMER);

        when(jwtService.extractUsername(token)).thenReturn("customer@test.com");
        when(userService.getUserByEmail("customer@test.com")).thenReturn(user);
        when(customerProfileRepository.findByUserId(customerUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getAllInvoices(token, pageable, filter))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer profile not found");
    }

    @Test
    void getAllInvoices_freelancerProfileNotFound_shouldThrow() {
        String token = "jwt.token";
        Pageable pageable = PageRequest.of(0, 10);
        InvoiceFilterDTO filter = InvoiceFilterDTO.builder().build();

        User user = new User();
        user.setId(freelancerUserId);
        user.setRole(Role.STAFF);

        when(jwtService.extractUsername(token)).thenReturn("freelancer@test.com");
        when(userService.getUserByEmail("freelancer@test.com")).thenReturn(user);
        when(freelancerProfileRepository.findByUserId(freelancerUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getAllInvoices(token, pageable, filter))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Freelancer profile not found");
    }


    @Test
    void getInvoiceById_found_shouldReturnDetail() {
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        when(contractMapper.toDetailDto(
                any(), any(), any(), any(), any(), any()))
                .thenReturn(ContractDetailDTO.builder().id(UUID.randomUUID()).build());

        when(milestoneMapper.toDto(milestone))
                .thenReturn(MilestoneResponseDTO.builder().id(UUID.randomUUID()).build());

        when(invoiceMapper.toDetailDto(any(), any(), any()))
                .thenReturn(InvoiceDetailDTO.builder().id(UUID.randomUUID()).build());

        InvoiceDetailDTO result = invoiceService.getInvoiceById(invoiceId);
        assertThat(result).isNotNull();
    }

    @Test
    void getInvoiceById_notFound_shouldThrow() {
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> invoiceService.getInvoiceById(invoiceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Invoice not found.");
    }

    @Test
    void createInvoice_withMilestone_success() {
        InvoiceRequestDTO request = InvoiceRequestDTO.builder()
                .contractId(contractId)
                .milestoneId(milestoneId)
                .build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(milestoneRepository.findById(milestoneId)).thenReturn(Optional.of(milestone));
        when(invoiceRepository.save(any())).thenReturn(invoice);

        when(milestoneMapper.toDto(milestone))
                .thenReturn(MilestoneResponseDTO.builder().id(UUID.randomUUID()).build());

        when(contractMapper.toDetailDto(any(), any(), any(), any(), any(), any()))
                .thenReturn(ContractDetailDTO.builder().id(UUID.randomUUID()).build());

        when(invoiceMapper.toDetailDto(any(), any(), any()))
                .thenReturn(InvoiceDetailDTO.builder().id(UUID.randomUUID()).build());

        InvoiceDetailDTO result = invoiceService.createInvoice(request);

        assertThat(result).isNotNull();
        verify(milestoneRepository).save(milestone);
    }

    @Test
    void createInvoice_withoutMilestone_shouldUseContractAmount() {
        InvoiceRequestDTO request = InvoiceRequestDTO.builder()
                .contractId(contractId)
                .build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(invoiceRepository.save(any())).thenReturn(invoice);

        when(contractMapper.toDetailDto(any(), any(), any(), any(), any(), any()))
                .thenReturn(ContractDetailDTO.builder().id(UUID.randomUUID()).build());

        when(invoiceMapper.toDetailDto(any(), any(), any()))
                .thenReturn(InvoiceDetailDTO.builder().id(UUID.randomUUID()).build());

        InvoiceDetailDTO result = invoiceService.createInvoice(request);

        assertThat(result).isNotNull();
    }

    @Test
    void createInvoice_contractNotFound_shouldThrow() {
        InvoiceRequestDTO request = InvoiceRequestDTO.builder()
                .contractId(contractId)
                .build();
        when(contractRepository.findById(contractId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> invoiceService.createInvoice(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createInvoice_milestoneNotFound_shouldThrow() {
        InvoiceRequestDTO request = InvoiceRequestDTO.builder()
                .contractId(contractId)
                .milestoneId(milestoneId)
                .build();
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(milestoneRepository.findById(milestoneId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.createInvoice(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Milestone not found.");
    }

    @Test
    void createInvoice_milestoneNotInContract_shouldThrow() {
        Milestone otherMilestone = new Milestone();
        otherMilestone.setId(UUID.randomUUID());

        InvoiceRequestDTO request = InvoiceRequestDTO.builder()
                .contractId(contractId)
                .milestoneId(otherMilestone.getId())
                .build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(milestoneRepository.findById(otherMilestone.getId())).thenReturn(Optional.of(otherMilestone));

        assertThatThrownBy(() -> invoiceService.createInvoice(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Milestone does not belong");
    }

    @Test
    void updateInvoice_replacePayment_shouldThrow() {
        Payment payment = new Payment();
        invoice.setPayment(payment);

        InvoiceRequestDTO request = InvoiceRequestDTO.builder().payment(new Payment()).build();
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.updateInvoice(invoiceId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot replace an existing payment.");
    }

    @Test
    void updateInvoice_clearPayment_success() {
        Payment payment = new Payment();
        invoice.setPayment(payment);

        InvoiceRequestDTO request = InvoiceRequestDTO.builder().payment(null).build();
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);

        when(contractMapper.toDetailDto(any(), any(), any(), any(), any(), any()))
                .thenReturn(ContractDetailDTO.builder().id(UUID.randomUUID()).build());

        when(invoiceMapper.toDetailDto(any(), any(), any()))
                .thenReturn(InvoiceDetailDTO.builder().id(UUID.randomUUID()).build());

        InvoiceDetailDTO result = invoiceService.updateInvoice(invoiceId, request);

        assertThat(result).isNotNull();
        assertThat(invoice.getPayment()).isNull();
    }

    @Test
    void updateInvoice_addPayment_success() {
        Payment payment = new Payment();
        InvoiceRequestDTO request = InvoiceRequestDTO.builder()
                .payment(payment)
                .build();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);

        when(contractMapper.toDetailDto(any(), any(), any(), any(), any(), any()))
                .thenReturn(ContractDetailDTO.builder().id(UUID.randomUUID()).build());

        when(invoiceMapper.toDetailDto(any(), any(), any()))
                .thenReturn(InvoiceDetailDTO.builder().id(UUID.randomUUID()).build());

        InvoiceDetailDTO result = invoiceService.updateInvoice(invoiceId, request);

        assertThat(result).isNotNull();
        assertThat(invoice.getPayment()).isEqualTo(payment);
    }

    @Test
    void updateInvoiceStatusById_paid_milestone() {
        InvoiceStatusRequestDTO request = InvoiceStatusRequestDTO.builder()
                .status(InvoiceStatus.PAID)
                .build();

        invoice.setMilestone(milestone);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        when(milestoneService.updateMilestoneStatusById(any(UUID.class), any()))
                .thenReturn(MilestoneResponseDTO.builder().id(UUID.randomUUID()).build());

        when(invoiceRepository.save(invoice)).thenReturn(invoice);
        when(contractMapper.toDetailDto(any(), any(), any(), any(), any(), any()))
                .thenReturn(ContractDetailDTO.builder().id(UUID.randomUUID()).build());

        when(invoiceMapper.toDetailDto(any(), any(), any()))
                .thenReturn(InvoiceDetailDTO.builder().id(UUID.randomUUID()).build());

        InvoiceDetailDTO result = invoiceService.updateInvoiceStatusById(invoiceId, request);

        assertThat(result).isNotNull();
        verify(milestoneService).updateMilestoneStatusById(eq(milestone.getId()), any());
    }

    @Test
    void updateInvoiceStatusById_paid_noMilestone() {
        invoice.setMilestone(null);

        InvoiceStatusRequestDTO request = InvoiceStatusRequestDTO.builder()
                .status(InvoiceStatus.PAID)
                .build();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        when(contractService.updateContractStatusById(eq(contractId), any())).thenReturn(null);

        when(invoiceRepository.save(invoice)).thenReturn(invoice);

        when(contractMapper.toDetailDto(any(), any(), any(), any(), any(), any()))
                .thenReturn(ContractDetailDTO.builder().id(UUID.randomUUID()).build());

        when(invoiceMapper.toDetailDto(any(), any(), any()))
                .thenReturn(InvoiceDetailDTO.builder().id(UUID.randomUUID()).build());

        InvoiceDetailDTO result = invoiceService.updateInvoiceStatusById(invoiceId, request);

        assertThat(result).isNotNull();
        verify(contractService).updateContractStatusById(eq(contractId), any());
    }

    @Test
    void updateInvoiceStatusById_cancelled_withMilestone() {
        InvoiceStatusRequestDTO request = InvoiceStatusRequestDTO.builder()
                .status(InvoiceStatus.CANCELLED)
                .build();
        invoice.setMilestone(milestone);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);
        when(milestoneService.updateMilestoneStatusById(any(), any())).thenReturn(
                MilestoneResponseDTO.builder().id(UUID.randomUUID()).build()
        );

        invoiceService.updateInvoiceStatusById(invoiceId, request);

        verify(milestoneService).updateMilestoneStatusById(eq(milestoneId), any());
    }

    @Test
    void updateInvoiceStatusById_pending_noMilestone() {
        InvoiceStatusRequestDTO request = InvoiceStatusRequestDTO.builder()
                .status(InvoiceStatus.PENDING)
                .build();
        invoice.setMilestone(null);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);
        when(contractService.updateContractStatusById(eq(contractId), any())).thenReturn(null);

        invoiceService.updateInvoiceStatusById(invoiceId, request);

        verify(contractService).updateContractStatusById(eq(contractId), any());
    }

    @Test
    void updateInvoiceStatus_invoiceWithoutMilestone_shouldUpdateContractStatus() {
        UUID invoiceId = UUID.randomUUID();

        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setContract(contract);
        invoice.setMilestone(null);

        InvoiceStatusRequestDTO request = InvoiceStatusRequestDTO.builder()
                .status(InvoiceStatus.CANCELLED)
                .build();

        given(invoiceRepository.findById(invoiceId)).willReturn(Optional.of(invoice));
        given(contractRepository.findById(invoice.getContract().getId())).willReturn(Optional.of(invoice.getContract()));
        given(invoiceRepository.save(any(Invoice.class))).willAnswer(i -> i.getArgument(0));

        ContractDetailDTO contractDto = ContractDetailDTO.builder().id(contract.getId()).build();
        when(contractService.updateContractStatusById(eq(contract.getId()), any()))
                .thenReturn(contractDto);

        when(contractMapper.toDetailDto(any(), any(), any(), any(), any(), any()))
                .thenReturn(contractDto);
        when(milestoneMapper.toDto(any()))
                .thenReturn(MilestoneResponseDTO.builder().id(UUID.randomUUID()).build());
        when(invoiceMapper.toDetailDto(any(), any(), any()))
                .thenAnswer(invocation -> {
                    Invoice inv = invocation.getArgument(0);
                    return InvoiceDetailDTO.builder()
                            .id(inv.getId())
                            .status(inv.getStatus())
                            .build();
                });

        InvoiceDetailDTO result = invoiceService.updateInvoiceStatusById(invoiceId, request);

        verify(contractService).updateContractStatusById(
                eq(invoice.getContract().getId()),
                argThat(dto -> dto.getStatus() == ContractStatus.ACTIVE)
        );

        assertNotNull(result);
        assertEquals(InvoiceStatus.CANCELLED, result.getStatus());
    }

    @Test
    void updateInvoiceStatus_invoiceWithMilestone_shouldUpdateMilestoneStatus() {
        InvoiceStatusRequestDTO request = InvoiceStatusRequestDTO.builder()
                .status(InvoiceStatus.CANCELLED)
                .build();

        invoice.setMilestone(milestone);

        given(invoiceRepository.findById(invoiceId)).willReturn(Optional.of(invoice));
        given(contractRepository.findById(invoice.getContract().getId())).willReturn(Optional.of(invoice.getContract()));
        given(invoiceRepository.save(any(Invoice.class))).willAnswer(i -> i.getArgument(0));

        MilestoneResponseDTO milestoneDto = MilestoneResponseDTO.builder().id(milestone.getId()).build();
        when(milestoneService.updateMilestoneStatusById(eq(milestone.getId()), any()))
                .thenReturn(milestoneDto);

        when(contractMapper.toDetailDto(any(), any(), any(), any(), any(), any()))
                .thenReturn(ContractDetailDTO.builder().id(contract.getId()).build());
        when(milestoneMapper.toDto(any()))
                .thenReturn(milestoneDto);
        when(invoiceMapper.toDetailDto(any(), any(), any()))
                .thenAnswer(invocation -> {
                    Invoice inv = invocation.getArgument(0);
                    return InvoiceDetailDTO.builder()
                            .id(inv.getId())
                            .status(inv.getStatus())
                            .build();
                });

        InvoiceDetailDTO result = invoiceService.updateInvoiceStatusById(invoiceId, request);

        verify(milestoneService).updateMilestoneStatusById(
                eq(milestone.getId()),
                argThat(dto -> dto.getStatus() == MilestoneStatus.PENDING)
        );

        assertNotNull(result);
        assertEquals(InvoiceStatus.CANCELLED, result.getStatus());
    }

    @Test
    void deleteInvoice_paid_shouldThrow() {
        invoice.setPayment(new Payment());
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.deleteInvoice(invoiceId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot delete an invoice that has been paid.");
    }

    @Test
    void deleteInvoice_milestone_shouldUpdateMilestone() {
        invoice.setMilestone(milestone);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        doNothing().when(invoiceRepository).delete(invoice);

        when(milestoneService.updateMilestoneStatusById(any(), any()))
                .thenReturn(MilestoneResponseDTO.builder().id(UUID.randomUUID()).build());

        invoiceService.deleteInvoice(invoiceId);

        verify(milestoneService).updateMilestoneStatusById(any(), any());
    }

    @Test
    void deleteInvoice_noMilestone_shouldUpdateContract() {
        invoice.setMilestone(null);
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        doNothing().when(invoiceRepository).delete(invoice);
        when(contractService.updateContractStatusById(eq(contractId), any())).thenReturn(null);

        invoiceService.deleteInvoice(invoiceId);

        verify(contractService).updateContractStatusById(eq(contractId), any());
    }

    @Test
    void getFreelancerId_nullUserId_shouldThrow() throws Exception {
        InvoiceServiceImpl service = new InvoiceServiceImpl(
                invoiceRepository, invoiceMapper, contractMapper, milestoneMapper, jwtService,
                contractRepository, milestoneRepository, contractService, milestoneService,
                addressMapper, userService, freelancerProfileRepository, customerProfileRepository
        );

        var method = InvoiceServiceImpl.class.getDeclaredMethod("getFreelancerId", UUID.class);
        method.setAccessible(true);

        Throwable thrown = catchThrowable(() -> method.invoke(service, new Object[]{null}));

        assertThat(thrown)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);

        assertThat(thrown.getCause())
                .hasMessageContaining("User ID cannot be null");
    }

    @Test
    void updateInvoiceStatus_invoiceWithMilestone_pending_shouldCallMilestoneService() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setContract(contract);
        invoice.setMilestone(milestone);

        given(invoiceRepository.findById(invoice.getId())).willReturn(Optional.of(invoice));
        given(contractRepository.findById(contract.getId())).willReturn(Optional.of(contract));
        given(invoiceRepository.save(any())).willAnswer(i -> i.getArgument(0));
        when(milestoneMapper.toDto(any())).thenReturn(MilestoneResponseDTO.builder().id(milestone.getId()).build());
        when(invoiceMapper.toDetailDto(any(), any(), any()))
                .thenAnswer(invocation -> InvoiceDetailDTO.builder()
                        .id(invoice.getId())
                        .status(invoice.getStatus())
                        .build());

        InvoiceStatusRequestDTO request = InvoiceStatusRequestDTO.builder()
                .status(InvoiceStatus.PENDING)
                .build();

        InvoiceDetailDTO result = invoiceService.updateInvoiceStatusById(invoice.getId(), request);

        verify(milestoneService).updateMilestoneStatusById(
                eq(milestone.getId()),
                argThat(dto -> dto.getStatus() == MilestoneStatus.PENDING)
        );
        assertNotNull(result);
    }

    @Test
    void getCustomerId_nullUserId_shouldThrow() throws Exception {
        InvoiceServiceImpl service = new InvoiceServiceImpl(
                invoiceRepository, invoiceMapper, contractMapper, milestoneMapper, jwtService,
                contractRepository, milestoneRepository, contractService, milestoneService,
                addressMapper, userService, freelancerProfileRepository, customerProfileRepository
        );

        var method = InvoiceServiceImpl.class.getDeclaredMethod("getCustomerId", UUID.class);
        method.setAccessible(true);

        Throwable thrown = catchThrowable(() -> method.invoke(service, new Object[]{null}));

        assertThat(thrown).isInstanceOf(InvocationTargetException.class);

        assertThat(thrown.getCause())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID cannot be null");
    }
}