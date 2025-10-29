package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.ContractMapper;
import com.jobmatcher.server.mapper.InvoiceMapper;
import com.jobmatcher.server.mapper.MilestoneMapper;
import com.jobmatcher.server.model.*;
import com.jobmatcher.server.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContractServiceImplTest {

    @Mock ContractRepository contractRepository;
    @Mock IProposalService proposalService;
    @Mock ContractMapper contractMapper;
    @Mock ICustomerProfileService customerService;
    @Mock IFreelancerProfileService freelancerService;
    @Mock IUserService userService;
    @Mock IProjectService projectService;
    @Mock InvoiceRepository invoiceRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock FreelancerProfileRepository freelancerProfileRepository;
    @Mock CustomerProfileRepository customerProfileRepository;
    @Mock JwtService jwtService;
    @Mock InvoiceMapper invoiceMapper;
    @Mock MilestoneMapper milestoneMapper;

    @InjectMocks
    ContractServiceImpl service;

    UUID contractId;
    Contract contract;
    ContractRequestDTO contractRequest;
    ContractStatusRequestDTO contractStatusRequest;
    Project project;
    User user;

    @BeforeEach
    void setUp() {
        contractId = UUID.randomUUID();
        contract = new Contract();
        contract.setId(contractId);
        contract.setInvoices(new HashSet<>());
        contract.setMilestones(new HashSet<>());
        contract.setAmount(BigDecimal.valueOf(1000));
        project = new Project();
        project.setId(UUID.randomUUID());
        contract.setProject(project);

        contractRequest = ContractRequestDTO.builder().build();
        contractStatusRequest = ContractStatusRequestDTO.builder().status(ContractStatus.COMPLETED).build();

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
    }

    @Test
    void getContractById_found_returnsDetail() {
        CustomerProfile customer = new CustomerProfile();
        customer.setId(UUID.randomUUID());

        FreelancerProfile freelancer = new FreelancerProfile();
        freelancer.setId(UUID.randomUUID());

        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setCustomer(customer);
        project.setFreelancer(freelancer);

        contract.setProject(project);

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        CustomerDetailDTO customerDTO = CustomerDetailDTO.builder()
                .userId(UUID.randomUUID())
                .build();
        FreelancerDetailDTO freelancerDTO = FreelancerDetailDTO.builder()
                .userId(UUID.randomUUID())
                .build();
        UserResponseDTO customerUser = UserResponseDTO.builder().build();
        UserResponseDTO freelancerUser = UserResponseDTO.builder().build();

        when(customerService.getCustomerProfileById(customer.getId())).thenReturn(customerDTO);
        when(freelancerService.getFreelancerProfileById(freelancer.getId())).thenReturn(freelancerDTO);
        when(userService.getUserById(customerDTO.getUserId())).thenReturn(customerUser);
        when(userService.getUserById(freelancerDTO.getUserId())).thenReturn(freelancerUser);
        when(contractMapper.toDetailDto(any(), any(), any(), any(), any(), any()))
                .thenReturn(ContractDetailDTO.builder().build());

        ContractDetailDTO result = service.getContractById(contractId);

        assertNotNull(result);
    }

    @Test
    void getAllContracts_customer_returnsContracts() {
        String token = "dummyToken";
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(Role.CUSTOMER);
        user.setEmail("test@example.com");

        CustomerProfile customer = new CustomerProfile();
        customer.setId(UUID.randomUUID());

        when(jwtService.extractUsername(token)).thenReturn(user.getEmail());
        when(userService.getUserByEmail(user.getEmail())).thenReturn(user);
        when(customerProfileRepository.findByUserId(user.getId())).thenReturn(Optional.of(customer));
        when(contractRepository.findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        Page<ContractSummaryDTO> result = service.getAllContracts(token, Pageable.unpaged(), null);
        assertNotNull(result);
    }

    @Test
    void getAllContracts_staff_returnsContracts() {
        String token = "dummyToken";
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(Role.STAFF);
        user.setEmail("staff@example.com");

        FreelancerProfile freelancer = new FreelancerProfile();
        freelancer.setId(UUID.randomUUID());

        when(jwtService.extractUsername(token)).thenReturn(user.getEmail());
        when(userService.getUserByEmail(user.getEmail())).thenReturn(user);
        when(freelancerProfileRepository.findByUserId(user.getId())).thenReturn(Optional.of(freelancer));

        when(contractRepository.findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        Page<ContractSummaryDTO> result = service.getAllContracts(token, Pageable.unpaged(), null);
        assertNotNull(result);
    }

    @Test
    void getAllContracts_customerUserIdNull_throwsIllegalArgumentException() {
        String token = "dummyToken";
        User newUser = new User();
        newUser.setRole(Role.CUSTOMER);

        when(jwtService.extractUsername(token)).thenReturn("customer@test.com");
        when(userService.getUserByEmail("customer@test.com"))
                .thenReturn(newUser);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.getAllContracts(token, Pageable.unpaged(), null));

        assertEquals("User ID cannot be null", exception.getMessage());
    }

    @Test
    void getAllContracts_staffUserIdNull_throwsIllegalArgumentException() {
        String token = "dummyToken";
        User newUser = new User();
        newUser.setRole(Role.STAFF);

        when(jwtService.extractUsername(token)).thenReturn("staff@test.com");
        when(userService.getUserByEmail("staff@test.com"))
                .thenReturn(newUser);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.getAllContracts(token, Pageable.unpaged(), null));

        assertEquals("User ID cannot be null", exception.getMessage());
    }


    @Test
    void getContractById_notFound_throws() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getContractById(contractId));
    }

    @Test
    void getContractByProjectId_found_returnsDetail() {
        Project project = new Project();
        CustomerProfile customer = new CustomerProfile();
        customer.setId(UUID.randomUUID());
        FreelancerProfile freelancer = new FreelancerProfile();
        freelancer.setId(UUID.randomUUID());
        project.setCustomer(customer);
        project.setFreelancer(freelancer);

        Contract contract = new Contract();
        contract.setProject(project);

        when(contractRepository.findByProjectId(project.getId())).thenReturn(Optional.of(contract));
        when(customerService.getCustomerProfileById(customer.getId())).thenReturn(CustomerDetailDTO.builder().userId(UUID.randomUUID()).build());
        when(freelancerService.getFreelancerProfileById(freelancer.getId())).thenReturn(FreelancerDetailDTO.builder().userId(UUID.randomUUID()).build());
        when(userService.getUserById(any())).thenReturn(UserResponseDTO.builder().build());
        when(contractMapper.toDetailDto(any(), any(), any(), any(), any(), any())).thenReturn(ContractDetailDTO.builder().build());

        ContractDetailDTO dto = service.getContractByProjectId(project.getId());
        assertNotNull(dto);
    }

    @Test
    void getAllContracts_freelancerProfileNotFound_throws() {
        String token = "dummyToken";
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(Role.STAFF);

        when(jwtService.extractUsername(token)).thenReturn(user.getEmail());
        when(userService.getUserByEmail(user.getEmail())).thenReturn(user);
        when(freelancerProfileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getAllContracts(token, Pageable.unpaged(), null));
    }

    @Test
    void getAllContracts_customerProfileNotFound_throws() {
        String token = "dummyToken";

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(Role.CUSTOMER);
        user.setEmail("test@example.com");

        when(jwtService.extractUsername(token)).thenReturn(user.getEmail());
        when(userService.getUserByEmail(user.getEmail())).thenReturn(user);

        when(customerProfileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getAllContracts(token, Pageable.unpaged(), null));
    }

    @Test
    void getContractDetailData_withMilestonesAndInvoices_mapsProperly() {
        UUID contractId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID freelancerId = UUID.randomUUID();
        UUID customerUserId = UUID.randomUUID();
        UUID freelancerUserId = UUID.randomUUID();

        Milestone milestone = new Milestone();
        milestone.setId(UUID.randomUUID());

        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setMilestone(milestone);

        CustomerProfile customerProfile = new CustomerProfile();
        customerProfile.setId(customerId);
        FreelancerProfile freelancerProfile = new FreelancerProfile();
        freelancerProfile.setId(freelancerId);

        Project project = new Project();
        project.setCustomer(customerProfile);
        project.setFreelancer(freelancerProfile);
        project.setPaymentType(PaymentType.MILESTONE);

        Contract contract = new Contract();
        contract.setId(contractId);
        contract.setProject(project);
        contract.setMilestones(Set.of(milestone));
        contract.setInvoices(Set.of(invoice));

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        when(customerService.getCustomerProfileById(customerId))
                .thenReturn(CustomerDetailDTO.builder().userId(customerUserId).build());
        when(freelancerService.getFreelancerProfileById(freelancerId))
                .thenReturn(FreelancerDetailDTO.builder().userId(freelancerUserId).build());

        UserResponseDTO customerUser = UserResponseDTO.builder().email("customer@test.com").phone("123").build();
        UserResponseDTO freelancerUser = UserResponseDTO.builder().email("freelancer@test.com").phone("456").build();
        when(userService.getUserById(customerUserId)).thenReturn(customerUser);
        when(userService.getUserById(freelancerUserId)).thenReturn(freelancerUser);

        when(milestoneMapper.toDto(any(Milestone.class)))
                .thenAnswer(i -> MilestoneResponseDTO.builder().id(((Milestone) i.getArgument(0)).getId()).build());
        when(contractMapper.toSummaryDto(any(Contract.class)))
                .thenAnswer(i -> ContractSummaryDTO.builder().id(((Contract) i.getArgument(0)).getId()).build());
        when(invoiceMapper.toSummaryDto(any(), any(), any()))
                .thenAnswer(i -> InvoiceSummaryDTO.builder().id(((Invoice) i.getArgument(0)).getId()).build());

        when(contractMapper.toDetailDto(
                any(Contract.class),
                any(ContactDTO.class),
                any(ContactDTO.class),
                anySet(),
                anySet(),
                any(PaymentType.class)
        )).thenAnswer(invocation -> {
            Contract c = invocation.getArgument(0);
            ContactDTO customerContact = invocation.getArgument(1);
            ContactDTO freelancerContact = invocation.getArgument(2);
            Set<InvoiceSummaryDTO> invoicesList = invocation.getArgument(3);
            Set<MilestoneResponseDTO> milestonesList = invocation.getArgument(4);
            PaymentType paymentType = invocation.getArgument(5);

            return ContractDetailDTO.builder()
                    .id(c.getId())
                    .customerId(c.getProject().getCustomer().getId())
                    .freelancerId(c.getProject().getFreelancer().getId())
                    .customerContact(customerContact)
                    .freelancerContact(freelancerContact)
                    .invoices(invoicesList)
                    .milestones(milestonesList)
                    .paymentType(paymentType)
                    .build();
        });

        ContractDetailDTO dto = service.getContractById(contractId);

        assertNotNull(dto);
        assertNotNull(dto.getMilestones());
        assertEquals(1, dto.getMilestones().size());
        assertEquals(milestone.getId(), dto.getMilestones().iterator().next().getId());

        assertNotNull(dto.getInvoices());
        assertEquals(1, dto.getInvoices().size());
        assertEquals(invoice.getId(), dto.getInvoices().iterator().next().getId());

        assertNotNull(dto.getCustomerContact());
        assertEquals(customerUser.getEmail(), dto.getCustomerContact().getEmail());

        assertNotNull(dto.getFreelancerContact());
        assertEquals(freelancerUser.getEmail(), dto.getFreelancerContact().getEmail());

        assertEquals(PaymentType.MILESTONE, dto.getPaymentType());
    }

    @Test
    void updateContractById_invoiceAndPaymentAndTotalPaid_updated() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());

        CustomerProfile customer = new CustomerProfile();
        customer.setId(UUID.randomUUID());
        FreelancerProfile freelancer = new FreelancerProfile();
        freelancer.setId(UUID.randomUUID());
        project.setCustomer(customer);
        project.setFreelancer(freelancer);
        contract.setProject(project);

        contractRequest.setInvoiceId(invoice.getId());
        contractRequest.setPaymentId(payment.getId());
        contractRequest.setTotalPaid(BigDecimal.valueOf(500));

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(contractRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(customerService.getCustomerProfileById(any())).thenReturn(CustomerDetailDTO.builder().build());
        when(freelancerService.getFreelancerProfileById(any())).thenReturn(FreelancerDetailDTO.builder().build());
        when(userService.getUserById(any())).thenReturn(UserResponseDTO.builder().build());
        when(invoiceMapper.toSummaryDto(any(Invoice.class), any(ContractSummaryDTO.class), any()))
                .thenReturn(InvoiceSummaryDTO.builder().build());
        when(milestoneMapper.toDto(any(Milestone.class)))
                .thenReturn(MilestoneResponseDTO.builder().build());
        when(contractMapper.toDetailDto(any(), any(), any(), any(), any(), any())).thenReturn(ContractDetailDTO.builder().build());

        ContractDetailDTO updated = service.updateContractById(contractId, contractRequest);

        assertNotNull(updated);
        assertTrue(contract.getInvoices().contains(invoice));
        assertEquals(payment, contract.getPayment());
        assertEquals(BigDecimal.valueOf(500), contract.getTotalPaid());
        assertEquals(BigDecimal.valueOf(500), contract.getRemainingBalance());
    }

    @Test
    void updateContractById_invoiceNotFound_throws() {
        contractRequest.setInvoiceId(UUID.randomUUID());
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(invoiceRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateContractById(contractId, contractRequest));
    }

    @Test
    void updateContractById_paymentNotFound_throws() {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        contractRequest.setPaymentId(payment.getId());
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(paymentRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateContractById(contractId, contractRequest));
    }

    @Test
    void updateContractStatusById_completed_updatesProjectStatus() {
        CustomerProfile customer = new CustomerProfile();
        customer.setId(UUID.randomUUID());

        FreelancerProfile freelancer = new FreelancerProfile();
        freelancer.setId(UUID.randomUUID());

        project.setId(UUID.randomUUID());
        project.setCustomer(customer);
        project.setFreelancer(freelancer);

        contract.setProject(project);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(contractRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CustomerDetailDTO customerDTO = CustomerDetailDTO.builder()
                .userId(UUID.randomUUID())
                .build();
        FreelancerDetailDTO freelancerDTO = FreelancerDetailDTO.builder()
                .userId(UUID.randomUUID())
                .build();

        when(customerService.getCustomerProfileById(customer.getId())).thenReturn(customerDTO);
        when(freelancerService.getFreelancerProfileById(freelancer.getId())).thenReturn(freelancerDTO);

        UserResponseDTO customerUser = UserResponseDTO.builder()
                .email("customer@example.com")
                .phone("123456")
                .build();
        UserResponseDTO freelancerUser = UserResponseDTO.builder()
                .email("freelancer@example.com")
                .phone("654321")
                .build();
        when(userService.getUserById(customerDTO.getUserId())).thenReturn(customerUser);
        when(userService.getUserById(freelancerDTO.getUserId())).thenReturn(freelancerUser);

        service.updateContractStatusById(contractId, contractStatusRequest);

        assertEquals(ContractStatus.COMPLETED, contract.getStatus());
        verify(projectService).updateProjectStatus(eq(project.getId()), any(ProjectStatusUpdateDTO.class));
    }

    @Test
    void updateContractById_completedAndTerminated_setsDates() {
        ContractRequestDTO request = ContractRequestDTO.builder()
                .completedAt(OffsetDateTime.now())
                .terminatedAt(OffsetDateTime.now())
                .build();
        Contract contract = new Contract();
        contract.setInvoices(new HashSet<>());
        contract.setMilestones(new HashSet<>());
        contract.setAmount(BigDecimal.valueOf(1000));
        Project project = new Project();
        CustomerProfile customer = new CustomerProfile();
        customer.setId(UUID.randomUUID());
        FreelancerProfile freelancer = new FreelancerProfile();
        freelancer.setId(UUID.randomUUID());
        project.setCustomer(customer);
        project.setFreelancer(freelancer);
        contract.setProject(project);

        when(contractRepository.findById(any())).thenReturn(Optional.of(contract));
        when(contractRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(customerService.getCustomerProfileById(any())).thenReturn(CustomerDetailDTO.builder().userId(UUID.randomUUID()).build());
        when(freelancerService.getFreelancerProfileById(any())).thenReturn(FreelancerDetailDTO.builder().userId(UUID.randomUUID()).build());
        when(userService.getUserById(any())).thenReturn(UserResponseDTO.builder().build());
        when(contractMapper.toDetailDto(any(), any(), any(), any(), any(), any())).thenReturn(ContractDetailDTO.builder().build());

        ContractDetailDTO updated = service.updateContractById(UUID.randomUUID(), request);
        assertNotNull(updated);
        assertNotNull(contract.getCompletedAt());
        assertNotNull(contract.getTerminatedAt());
    }

    @Test
    void updateContractStatusById_terminatedAndCancelled_setsStopped() {
        Contract contract = new Contract();
        contract.setInvoices(new HashSet<>());
        contract.setMilestones(new HashSet<>());

        CustomerProfile customer = new CustomerProfile();
        customer.setId(UUID.randomUUID());

        FreelancerProfile freelancer = new FreelancerProfile();
        freelancer.setId(UUID.randomUUID());

        Project project = new Project();
        project.setCustomer(customer);
        project.setFreelancer(freelancer);

        contract.setProject(project);

        ContractStatusRequestDTO terminatedRequest = ContractStatusRequestDTO.builder()
                .status(ContractStatus.TERMINATED)
                .build();
        ContractStatusRequestDTO cancelledRequest = ContractStatusRequestDTO.builder()
                .status(ContractStatus.CANCELLED)
                .build();

        when(contractRepository.findById(any())).thenReturn(Optional.of(contract));
        when(contractRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(projectService.updateProjectStatus(any(), any())).thenReturn(new ProjectDetailDTO());
        when(customerService.getCustomerProfileById(customer.getId()))
                .thenReturn(CustomerDetailDTO.builder().userId(UUID.randomUUID()).build());
        when(freelancerService.getFreelancerProfileById(freelancer.getId()))
                .thenReturn(FreelancerDetailDTO.builder().userId(UUID.randomUUID()).build());
        when(userService.getUserById(any())).thenReturn(UserResponseDTO.builder().build());
        when(contractMapper.toDetailDto(any(), any(), any(), any(), any(), any()))
                .thenReturn(ContractDetailDTO.builder().build());

        service.updateContractStatusById(UUID.randomUUID(), terminatedRequest);
        assertEquals(ContractStatus.TERMINATED, contract.getStatus());
        assertNotNull(contract.getTerminatedAt());

        service.updateContractStatusById(UUID.randomUUID(), cancelledRequest);
        assertEquals(ContractStatus.CANCELLED, contract.getStatus());
        assertNotNull(contract.getTerminatedAt());
    }

    @Test
    void updateContractStatusById_withActiveStatus_setsInProgressProjectStatus() {
        UUID contractId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        UUID customerId = UUID.randomUUID();
        UUID freelancerId = UUID.randomUUID();
        UUID customerUserId = UUID.randomUUID();
        UUID freelancerUserId = UUID.randomUUID();

        CustomerProfile customer = new CustomerProfile();
        customer.setId(customerId);
        FreelancerProfile freelancer = new FreelancerProfile();
        freelancer.setId(freelancerId);

        Project project = new Project();
        project.setId(projectId);
        project.setCustomer(customer);
        project.setFreelancer(freelancer);
        project.setPaymentType(PaymentType.MILESTONE);

        Contract contract = new Contract();
        contract.setId(contractId);
        contract.setProject(project);
        contract.setStatus(ContractStatus.ON_HOLD);

        ContractStatusRequestDTO request = ContractStatusRequestDTO.builder()
                .status(ContractStatus.ACTIVE)
                .build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(contractRepository.save(any(Contract.class))).thenAnswer(i -> i.getArgument(0));

        when(customerService.getCustomerProfileById(customerId))
                .thenReturn(CustomerDetailDTO.builder().userId(customerUserId).build());
        when(freelancerService.getFreelancerProfileById(freelancerId))
                .thenReturn(FreelancerDetailDTO.builder().userId(freelancerUserId).build());
        when(userService.getUserById(customerUserId)).thenReturn(UserResponseDTO.builder().email("customer@test.com").build());
        when(userService.getUserById(freelancerUserId)).thenReturn(UserResponseDTO.builder().email("freelancer@test.com").build());

        when(contractMapper.toSummaryDto(any(Contract.class)))
                .thenAnswer(i -> ContractSummaryDTO.builder().id(((Contract) i.getArgument(0)).getId()).build());
        when(milestoneMapper.toDto(any())).thenReturn(MilestoneResponseDTO.builder().build());
        when(invoiceMapper.toSummaryDto(any(), any(), any())).thenReturn(InvoiceSummaryDTO.builder().build());
        when(contractMapper.toDetailDto(any(), any(), any(), any(), any(), any()))
                .thenAnswer(i -> ContractDetailDTO.builder()
                        .id(((Contract) i.getArgument(0)).getId())
                        .customerContact(i.getArgument(1))
                        .freelancerContact(i.getArgument(2))
                        .milestones(Set.of())
                        .invoices(Set.of())
                        .paymentType(PaymentType.MILESTONE)
                        .build());

        ContractDetailDTO result = service.updateContractStatusById(contractId, request);

        assertNotNull(result);
        assertEquals(contractId, result.getId());
        verify(projectService).updateProjectStatus(
                eq(projectId),
                argThat(dto -> dto.getStatus() == ProjectStatus.IN_PROGRESS)
        );

        // ✅ Only one save() call for ACTIVE/default branch
        verify(contractRepository, times(1)).save(any(Contract.class));
        assertEquals(ContractStatus.ACTIVE, contract.getStatus());
    }

    @Test
    void deleteContractById_withInvoicesOrMilestones_throws() {
        contract.getInvoices().add(new Invoice());
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        assertThrows(IllegalStateException.class, () -> service.deleteContractById(contractId));
    }

    @Test
    void deleteContractById_projectWithProposals_deletesSuccessfully() {
        UUID contractId = UUID.randomUUID();
        Proposal proposal = new Proposal();
        proposal.setId(UUID.randomUUID());

        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setProposals(Set.of(proposal));

        Contract contract = new Contract();
        contract.setId(contractId);
        contract.setProject(project);

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        doReturn(ProposalDetailDTO.builder().build())
                .when(proposalService).updateProposalStatusById(any(UUID.class), any(ProposalStatusRequestDTO.class));

        doReturn(new ProjectDetailDTO())
                .when(projectService).updateProject(any(UUID.class), any(ProjectRequestDTO.class));

        doReturn(new ProjectDetailDTO())
                .when(projectService).updateProjectStatus(any(UUID.class), any(ProjectStatusUpdateDTO.class));

        service.deleteContractById(contractId);

        verify(contractRepository).delete(contract);

        verify(proposalService).updateProposalStatusById(
                eq(proposal.getId()),
                argThat(dto -> dto.getStatus() == ProposalStatus.PENDING)
        );

        verify(projectService).updateProject(eq(project.getId()), any(ProjectRequestDTO.class));
        verify(projectService).updateProjectStatus(eq(project.getId()), any(ProjectStatusUpdateDTO.class));
    }

    @Test
    void deleteContractById_withContractProposal_setsStatus() {
        Proposal proposal = new Proposal();
        Contract contract = new Contract();
        contract.setInvoices(Set.of());
        contract.setMilestones(Set.of());
        Project project = new Project();
        project.setProposals(Set.of(proposal));
        contract.setProject(project);
        contract.setProposal(proposal);

        when(contractRepository.findById(any())).thenReturn(Optional.of(contract));
        doReturn(ProposalDetailDTO.builder().build()).when(proposalService).updateProposalStatusById(any(), any());
        doReturn(new ProjectDetailDTO()).when(projectService).updateProject(any(), any());
        doReturn(new ProjectDetailDTO()).when(projectService).updateProjectStatus(any(), any());

        service.deleteContractById(UUID.randomUUID());

        assertEquals(ProposalStatus.REJECTED, contract.getProposal().getStatus());
    }

}
