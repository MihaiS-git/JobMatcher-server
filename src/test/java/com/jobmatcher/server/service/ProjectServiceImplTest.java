package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.exception.InvalidProjectOperationException;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.ProjectMapper;
import com.jobmatcher.server.model.*;
import com.jobmatcher.server.repository.*;
import com.jobmatcher.server.util.SanitizationUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private CustomerProfileRepository customerProfileRepository;
    @Mock
    private FreelancerProfileRepository freelancerProfileRepository;
    @Mock
    private JobCategoryRepository jobCategoryRepository;
    @Mock
    private JobSubcategoryRepository jobSubcategoryRepository;
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private ProposalRepository proposalRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private IUserService userService;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private UUID projectId;
    private Project project;
    private ProjectRequestDTO requestDto;
    private CustomerProfile customer;
    private JobCategory category;
    private Set<JobSubcategory> subcategories;

    @BeforeEach
    void setup() {
        projectId = UUID.randomUUID();

        project = new Project();
        project.setId(projectId);
        project.setTitle("Sample Project");
        project.setStatus(ProjectStatus.OPEN);
        project.setLastUpdate(OffsetDateTime.now());

        category = new JobCategory();
        category.setId(1L);
        category.setName("Development");

        JobSubcategory sub = new JobSubcategory();
        sub.setId(10L);
        subcategories = Set.of(sub);

        customer = new CustomerProfile();
        customer.setId(UUID.randomUUID());
        User user = new User();
        user.setRole(Role.CUSTOMER);
        customer.setUser(user);

        requestDto = ProjectRequestDTO.builder()
                .customerId(customer.getId())
                .categoryId(category.getId())
                .subcategoryIds(Set.of(10L))
                .title("Title")
                .description("Description")
                .budget(BigDecimal.valueOf(100))
                .paymentType(PaymentType.UPON_COMPLETION)
                .deadline(LocalDate.now().plusDays(3))
                .build();
    }

    // ───────────────────────────────────────────────
    // getProjectById
    // ───────────────────────────────────────────────
    @Test
    void getProjectById_shouldReturnDto_whenExists() {
        ProjectDetailDTO dto = ProjectDetailDTO.builder()
                .id(UUID.randomUUID())
                .title("Title")
                .description("Description")
                .budget(BigDecimal.valueOf(100))
                .paymentType(PaymentType.UPON_COMPLETION)
                .deadline(LocalDate.now().plusDays(3))
                .status(ProjectStatus.OPEN)
                .build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMapper.toDto(project)).thenReturn(dto);

        ProjectDetailDTO result = projectService.getProjectById(projectId);

        assertThat(result).isEqualTo(dto);
    }

    @Test
    void getProjectById_shouldThrow_whenNotFound() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> projectService.getProjectById(projectId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project not found");
    }

    // ───────────────────────────────────────────────
    // createProject
    // ───────────────────────────────────────────────
    @Test
    void createProject_shouldCreate_whenValid() {
        try (MockedStatic<SanitizationUtil> util = mockStatic(SanitizationUtil.class)) {
            util.when(() -> SanitizationUtil.sanitizeText(any())).thenAnswer(inv -> inv.getArgument(0));

            when(customerProfileRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(jobCategoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
            when(jobSubcategoryRepository.findAllById(any())).thenReturn(new ArrayList<>(subcategories));

            Project saved = new Project();
            ProjectDetailDTO dto = ProjectDetailDTO.builder()
                    .id(UUID.randomUUID())
                    .title("Title")
                    .description("Description")
                    .budget(BigDecimal.valueOf(100))
                    .paymentType(PaymentType.UPON_COMPLETION)
                    .deadline(LocalDate.now().plusDays(3))
                    .status(ProjectStatus.OPEN)
                    .build();
            when(projectMapper.toEntity(any(), eq(customer), isNull(), eq(category), any())).thenReturn(saved);
            when(projectRepository.save(saved)).thenReturn(saved);
            when(projectMapper.toDto(saved)).thenReturn(dto);

            ProjectDetailDTO result = projectService.createProject(requestDto);
            assertThat(result).isEqualTo(dto);
        }
    }

    @Test
    void createProject_shouldThrow_whenNoCustomerId() {
        requestDto.setCustomerId(null);
        assertThatThrownBy(() -> projectService.createProject(requestDto))
                .isInstanceOf(InvalidProjectOperationException.class);
    }

    @Test
    void createProject_shouldThrow_whenCustomerNotFound() {
        when(customerProfileRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> projectService.createProject(requestDto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createProject_shouldThrow_whenCustomerNotCustomerRole() {
        customer.getUser().setRole(Role.STAFF);
        when(customerProfileRepository.findById(any())).thenReturn(Optional.of(customer));
        assertThatThrownBy(() -> projectService.createProject(requestDto))
                .isInstanceOf(InvalidProjectOperationException.class);
    }

    @Test
    void createProject_shouldThrow_whenNoCategoryId() {
        requestDto.setCategoryId(null);
        when(customerProfileRepository.findById(any())).thenReturn(Optional.of(customer));
        assertThatThrownBy(() -> projectService.createProject(requestDto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createProject_shouldThrow_whenCategoryNotFound() {
        when(customerProfileRepository.findById(any())).thenReturn(Optional.of(customer));
        when(jobCategoryRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> projectService.createProject(requestDto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createProject_shouldThrow_whenNoSubcategories() {
        requestDto.setSubcategoryIds(Set.of());
        when(customerProfileRepository.findById(any())).thenReturn(Optional.of(customer));
        when(jobCategoryRepository.findById(any())).thenReturn(Optional.of(category));
        assertThatThrownBy(() -> projectService.createProject(requestDto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createProject_shouldThrow_whenDeadlinePast() {
        requestDto.setDeadline(LocalDate.now().minusDays(1));
        when(customerProfileRepository.findById(any())).thenReturn(Optional.of(customer));
        when(jobCategoryRepository.findById(any())).thenReturn(Optional.of(category));
        when(jobSubcategoryRepository.findAllById(any())).thenReturn(List.of());
        assertThatThrownBy(() -> projectService.createProject(requestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Deadline cannot be in the past");
    }

    // ───────────────────────────────────────────────
    // updateProject
    // ───────────────────────────────────────────────
    @Test
    void updateProject_shouldUpdateAllFields() {
        try (MockedStatic<SanitizationUtil> util = mockStatic(SanitizationUtil.class)) {
            util.when(() -> SanitizationUtil.sanitizeText(any())).thenAnswer(inv -> inv.getArgument(0));

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
            when(jobCategoryRepository.findById(any())).thenReturn(Optional.of(category));
            when(jobSubcategoryRepository.findAllById(any())).thenReturn(List.of(new JobSubcategory()));

            FreelancerProfile freelancer = new FreelancerProfile();
            freelancer.setId(UUID.randomUUID());
            Contract contract = new Contract();
            contract.setId(UUID.randomUUID());
            Proposal proposal = new Proposal();
            proposal.setId(UUID.randomUUID());

            requestDto.setFreelancerId(freelancer.getId());
            requestDto.setContractId(contract.getId());
            requestDto.setAcceptedProposalId(proposal.getId());

            when(freelancerProfileRepository.findById(freelancer.getId())).thenReturn(Optional.of(freelancer));
            when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
            when(proposalRepository.findById(proposal.getId())).thenReturn(Optional.of(proposal));

            when(projectRepository.save(project)).thenReturn(project);
            when(projectMapper.toDto(project)).thenReturn(new ProjectDetailDTO());

            ProjectDetailDTO result = projectService.updateProject(projectId, requestDto);
            assertThat(result).isNotNull();
            verify(projectRepository).save(project);
        }
    }

    @Test
    void updateProject_shouldThrow_whenProjectNotFound() {
        when(projectRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> projectService.updateProject(projectId, requestDto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProject_shouldThrow_whenDeadlineInvalid() {
        // Arrange
        requestDto.setDeadline(LocalDate.now()); // Invalid (today, not future)
        requestDto.setCategoryId(1L); // Required, or the code won’t reach deadline check

        when(projectRepository.findById(any())).thenReturn(Optional.of(project));
        when(jobCategoryRepository.findById(1L)).thenReturn(Optional.of(new JobCategory())); // Mock valid category
        when(jobSubcategoryRepository.findAllById(any())).thenReturn(List.of(new JobSubcategory())); // Avoid null pointer

        // Act + Assert
        assertThatThrownBy(() -> projectService.updateProject(projectId, requestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Deadline must be a future date.");
    }

    @Test
    void updateProject_shouldThrow_whenFreelancerNotFound() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        requestDto.setDeadline(LocalDate.now().plusDays(3));
        requestDto.setFreelancerId(UUID.randomUUID());
        assertThatThrownBy(() -> projectService.updateProject(projectId, requestDto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ───────────────────────────────────────────────
    // deleteProject
    // ───────────────────────────────────────────────
    @Test
    void deleteProject_shouldDelete_whenFound() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        projectService.deleteProject(projectId);
        verify(projectRepository).delete(project);
    }

    @Test
    void deleteProject_shouldThrow_whenNotFound() {
        when(projectRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> projectService.deleteProject(projectId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ───────────────────────────────────────────────
    // getAllJobFeedProjects
    // ───────────────────────────────────────────────
//    @Test
//    void getAllJobFeedProjects_shouldReturnPagedResponse() {
//        Pageable pageable = PageRequest.of(0, 2, Sort.by("title"));
//        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());
//        when(projectRepository.findFilteredJobFeedProjectIds(any(), any(), any(), any())).thenReturn(ids);
//        when(projectRepository.findByIdIn(ids))
//                .thenReturn(new ArrayList<>(List.of(project, project)));
//
//        when(projectMapper.toSummaryDto(any())).thenReturn(new ProjectSummaryDTO());
//
//        PagedResponseDTO<ProjectSummaryDTO> result = projectService.getAllJobFeedProjects(pageable, List.of(ProjectStatus.IN_PROGRESS), 1L, 2L, "term");
//        assertThat(result.content()).hasSize(2);
//    }

    // ───────────────────────────────────────────────
    // updateProjectStatus
    // ───────────────────────────────────────────────
    @Test
    void updateProjectStatus_shouldUpdate() {
        ProjectStatusUpdateDTO dto = ProjectStatusUpdateDTO.builder()
                .status(ProjectStatus.COMPLETED)
                .build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);
        when(projectMapper.toDto(project)).thenReturn(new ProjectDetailDTO());
        ProjectDetailDTO result = projectService.updateProjectStatus(projectId, dto);
        assertThat(result).isNotNull();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
    }

    @Test
    void updateProjectStatus_shouldThrow_whenNotFound() {
        when(projectRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> projectService.updateProjectStatus(UUID.randomUUID(), ProjectStatusUpdateDTO.builder()
                .status(ProjectStatus.OPEN)
                .build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ───────────────────────────────────────────────
    // sanitizeOptionalText
    // ───────────────────────────────────────────────
    @Test
    void sanitizeOptionalText_shouldThrow_whenInvalidSanitization() {
        try (MockedStatic<SanitizationUtil> util = mockStatic(SanitizationUtil.class)) {
            util.when(() -> SanitizationUtil.sanitizeText(any())).thenReturn(null);
            assertThatThrownBy(() -> {
                var m = ProjectServiceImpl.class.getDeclaredMethod("sanitizeOptionalText", String.class, String.class);
                m.setAccessible(true);
                m.invoke(null, "bad", "Title");
            }).hasCauseInstanceOf(IllegalArgumentException.class);
        } catch (Exception e) {
            fail("Unexpected reflection error", e);
        }
    }

//    @Test
//    void getAllProjects_asCustomer_shouldUseCustomerId() {
//        String token = "token";
//        UUID userId = UUID.randomUUID();
//        UUID customerId = UUID.randomUUID();
//
//        User user = new User();
//        user.setId(userId);
//        user.setRole(Role.CUSTOMER);
//
//        CustomerProfile customerProfile = new CustomerProfile();
//        customerProfile.setId(customerId);
//        customerProfile.setUser(user);
//
//        when(jwtService.extractUsername(token)).thenReturn("user@example.com");
//        when(userService.getUserByEmail("user@example.com")).thenReturn(user);
//        when(customerProfileRepository.findByUserId(userId))
//                .thenReturn(Optional.of(customerProfile));
//        when(projectRepository.findFilteredProjectIds(any(), any(), any(), any(), any()))
//                .thenReturn(List.of(UUID.randomUUID()));
//
//        Pageable pageable = PageRequest.of(0, 10);
//        when(projectRepository.findByIdIn(any())).thenReturn(new ArrayList<>(List.of(new Project())));
//        when(projectMapper.toSummaryDto(any())).thenReturn(new ProjectSummaryDTO());
//
//        projectService.getAllProjects(token, pageable, ProjectStatus.OPEN, 1L, 2L, "test");
//
//        verify(customerProfileRepository).findByUserId(userId);
//    }

//    @Test
//    void getAllProjects_asStaff_shouldUseFreelancerId() {
//        String token = "token";
//        UUID userId = UUID.randomUUID();
//        UUID freelancerId = UUID.randomUUID();
//
//        User user = new User();
//        user.setId(userId);
//        user.setRole(Role.STAFF);
//
//        FreelancerProfile freelancerProfile = new FreelancerProfile();
//        freelancerProfile.setId(freelancerId);
//        freelancerProfile.setUser(user);
//
//        when(jwtService.extractUsername(token)).thenReturn("staff@example.com");
//        when(userService.getUserByEmail("staff@example.com")).thenReturn(user);
//        when(freelancerProfileRepository.findByUserId(userId))
//                .thenReturn(Optional.of(freelancerProfile));
//        when(projectRepository.findFilteredProjectIds(any(), any(), any(), any(), any()))
//                .thenReturn(List.of(UUID.randomUUID()));
//
//        Pageable pageable = PageRequest.of(0, 10);
//        when(projectRepository.findByIdIn(any()))
//                .thenReturn(new ArrayList<>(List.of(new Project())));
//        when(projectMapper.toSummaryDto(any())).thenReturn(new ProjectSummaryDTO());
//
//        projectService.getAllProjects(token, pageable, ProjectStatus.OPEN, 1L, 2L, "test");
//
//        verify(freelancerProfileRepository).findByUserId(userId);
//    }

//    @Test
//    void getAllProjects_asOtherRole_shouldUseNullProfileId() {
//        User user = new User();
//        user.setRole(Role.ADMIN);
//
//        when(jwtService.extractUsername(any())).thenReturn("admin@example.com");
//        when(userService.getUserByEmail(any())).thenReturn(user);
//        when(projectRepository.findFilteredProjectIds(
//                isNull(), any(), any(), any(), any()
//        )).thenReturn(List.of(UUID.randomUUID()));
//
//        Pageable pageable = PageRequest.of(0, 10);
//        when(projectRepository.findByIdIn(any())).thenReturn(new ArrayList<>(List.of(new Project())));
//        when(projectMapper.toSummaryDto(any())).thenReturn(new ProjectSummaryDTO());
//
//        projectService.getAllProjects("token", pageable, ProjectStatus.OPEN, 1L, 2L, "test");
//
//        verify(projectRepository).findFilteredProjectIds(
//                isNull(), any(), any(), any(), any()
//        );
//    }

    @Test
    void updateProject_shouldClearAssociationsWhenIdsNull() {
        UUID projectId = UUID.randomUUID();
        Project existing = new Project();
        existing.setId(projectId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existing));
        when(projectRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(projectMapper.toDto(any())).thenReturn(ProjectDetailDTO.builder().id(projectId).build());

        ProjectRequestDTO dto = ProjectRequestDTO.builder().build();
        dto.setDeadline(LocalDate.now().plusDays(2)); // valid
        dto.setCategoryId(null);
        dto.setSubcategoryIds(null);

        ProjectDetailDTO result = projectService.updateProject(projectId, dto);

        assertThat(result.getId()).isEqualTo(projectId);
        verify(projectRepository).save(existing);
    }

//    @ParameterizedTest
//    @ValueSource(strings = {"status", "category", "deadline", "paymentType", "budget"})
//    void buildPageResponse_shouldSortByVariousFields(String field) throws Exception {
//        Pageable pageable = PageRequest.of(0, 5, Sort.by(field));
//
//        Project p1 = new Project();
//        p1.setId(UUID.randomUUID());
//        p1.setTitle("B");
//        p1.setStatus(ProjectStatus.IN_PROGRESS);
//        p1.setDeadline(LocalDate.now().plusDays(3));
//        p1.setBudget(BigDecimal.TEN);
//        p1.setPaymentType(PaymentType.UPON_COMPLETION);
//        JobCategory cat = new JobCategory();
//        cat.setName("Alpha");
//        p1.setCategory(cat);
//
//        Project p2 = new Project();
//        p2.setId(UUID.randomUUID());
//        p2.setTitle("A");
//        p2.setStatus(ProjectStatus.COMPLETED);
//        p2.setDeadline(LocalDate.now().plusDays(1));
//        p2.setBudget(BigDecimal.ONE);
//        p2.setPaymentType(PaymentType.MILESTONE);
//        JobCategory cat2 = new JobCategory();
//        cat2.setName("Beta");
//        p2.setCategory(cat2);
//
//        List<Project> projects = new ArrayList<>(List.of(p1, p2));
//
//        when(projectRepository.findByIdIn(any())).thenReturn(projects);
//        when(projectMapper.toSummaryDto(any())).thenReturn(new ProjectSummaryDTO());
//
//        PagedResponseDTO<ProjectSummaryDTO> result = invokePrivateBuildPageResponse(pageable, List.of(p1.getId(), p2.getId()));
//
//        assertThat(result.content()).hasSize(2);
//    }

//    @Test
//    void buildPageResponse_shouldUseDefaultComparatorWhenUnsorted() throws Exception {
//        Pageable pageable = PageRequest.of(0, 5);
//        Project p1 = new Project(); p1.setLastUpdate(OffsetDateTime.now().minusDays(1));
//        Project p2 = new Project(); p2.setLastUpdate(OffsetDateTime.now());
//        when(projectRepository.findByIdIn(any())).thenReturn(new ArrayList<>(List.of(p1, p2)));
//        when(projectMapper.toSummaryDto(any())).thenReturn(new ProjectSummaryDTO());
//
//        PagedResponseDTO<ProjectSummaryDTO> result = invokePrivateBuildPageResponse(pageable, List.of(UUID.randomUUID()));
//
//        assertThat(result).isNotNull();
//    }

    @SuppressWarnings("unchecked")
    private PagedResponseDTO<ProjectSummaryDTO> invokePrivateBuildPageResponse(Pageable pageable, List<UUID> ids) throws Exception {
        Method method = ProjectServiceImpl.class.getDeclaredMethod("buildPageResponse", Pageable.class, List.class);
        method.setAccessible(true);  // allow calling private method
        return (PagedResponseDTO<ProjectSummaryDTO>) method.invoke(projectService, pageable, ids);
    }


}