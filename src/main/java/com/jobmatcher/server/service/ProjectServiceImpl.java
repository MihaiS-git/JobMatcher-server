package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.exception.RoleAccessDeniedException;
import com.jobmatcher.server.mapper.ProjectMapper;
import com.jobmatcher.server.model.ProjectRequestDTO;
import com.jobmatcher.server.model.ProjectResponseDTO;
import com.jobmatcher.server.repository.*;
import com.jobmatcher.server.specification.ProjectSpecifications;
import com.jobmatcher.server.util.SanitizationUtil;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
public class ProjectServiceImpl implements IProjectService {

    private static final Set<ProjectStatus> STAFF_GLOBAL_STATUSES = EnumSet.of(
            ProjectStatus.OPEN, ProjectStatus.PROPOSALS_RECEIVED
    );
    private static final Set<ProjectStatus> STAFF_RESTRICTED_STATUSES = EnumSet.of(
            ProjectStatus.IN_PROGRESS, ProjectStatus.COMPLETED, ProjectStatus.CANCELLED
    );

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final CustomerProfileRepository customerProfileRepository;
    private final FreelancerProfileRepository freelancerProfileRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final JobSubcategoryRepository jobSubcategoryRepository;
    private final JwtService jwtService;
    private final IUserService userService;

    public ProjectServiceImpl(
            ProjectRepository projectRepository,
            ProjectMapper projectMapper,
            CustomerProfileRepository customerProfileRepository,
            FreelancerProfileRepository freelancerProfileRepository,
            JobCategoryRepository jobCategoryRepository,
            JobSubcategoryRepository jobSubcategoryRepository,
            JwtService jwtService,
            IUserService userService
    ) {
        this.projectRepository = projectRepository;
        this.projectMapper = projectMapper;
        this.customerProfileRepository = customerProfileRepository;
        this.freelancerProfileRepository = freelancerProfileRepository;
        this.jobCategoryRepository = jobCategoryRepository;
        this.jobSubcategoryRepository = jobSubcategoryRepository;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ProjectResponseDTO> getAllProjects(
            String token,
            Pageable pageable,
            UUID customerId,
            UUID freelancerId,
            ProjectStatus status,
            Long categoryId,
            Long subcategoryId,
            String searchTerm
    ) {
        Role role = getRole(token);

        switch (role) {
            case CUSTOMER -> {
                customerId = getCustomerId(token, customerId);
                Specification<Project> spec = ProjectSpecifications.filterProjects(
                        customerId, freelancerId, status, categoryId, subcategoryId, searchTerm
                );
                return projectRepository.findAll(spec, pageable).map(projectMapper::toSummaryDto);
            }
            case STAFF -> {
                Specification<Project> spec = buildStaffSpecification(
                        freelancerId, status, categoryId, subcategoryId, searchTerm
                );
                return projectRepository.findAll(spec, pageable).map(projectMapper::toSummaryDto);
            }
            case ADMIN -> {
                Specification<Project> spec = ProjectSpecifications.filterProjects(
                        customerId, freelancerId, status, categoryId, subcategoryId, searchTerm
                );
                return projectRepository.findAll(spec, pageable).map(projectMapper::toSummaryDto);
            }
            default -> throw new RoleAccessDeniedException("Unsupported role: " + role);
        }
    }

    @Override
    public ProjectResponseDTO getProjectById(UUID id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
        return projectMapper.toDto(project);
    }

    @Override
    public ProjectResponseDTO createProject(ProjectRequestDTO requestDto) {
        CustomerProfile customer = customerProfileRepository.findById(UUID.fromString(requestDto.getCustomerId()))
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + requestDto.getCustomerId()));
        JobCategory category = jobCategoryRepository.findById(requestDto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + requestDto.getCategoryId()));
        Set<JobSubcategory> subcategories = new HashSet<>(jobSubcategoryRepository.findAllById(requestDto.getSubcategoryIds()));

        if (requestDto.getDeadline().isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Deadline cannot be in the past");

        ProjectRequestDTO sanitizedRequest = sanitizeProjectRequest(requestDto);

        Project newProject = projectMapper.toEntity(sanitizedRequest, customer, null, category, subcategories);

        return projectMapper.toDto(projectRepository.save(newProject));
    }

    @Override
    public ProjectResponseDTO updateProject(UUID id, ProjectRequestDTO requestDto) {
        Project existingProject = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
        FreelancerProfile freelancer = requestDto.getFreelancerId() != null
                ? freelancerProfileRepository.findById(UUID.fromString(requestDto.getFreelancerId()))
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found with id: " + requestDto.getFreelancerId()))
                : null;
        JobCategory category = requestDto.getCategoryId() != null
                ? jobCategoryRepository.findById(requestDto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + requestDto.getCategoryId()))
                : null;
        Set<JobSubcategory> subcategories = requestDto.getSubcategoryIds() != null
                ? new HashSet<>(jobSubcategoryRepository.findAllById(requestDto.getSubcategoryIds()))
                : new HashSet<>();

        if (requestDto.getDeadline().isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Deadline cannot be in the past");

        ProjectRequestDTO sanitizedRequest = sanitizeProjectRequest(requestDto);

        if (freelancer != null) existingProject.setFreelancer(freelancer);
        updateIfPresent(sanitizedRequest.getTitle(), existingProject::setTitle);
        updateIfPresent(sanitizedRequest.getDescription(), existingProject::setDescription);
        updateIfPresent(requestDto.getStatus(), existingProject::setStatus);
        updateIfPresent(requestDto.getBudget(), existingProject::setBudget);
        updateIfPresent(requestDto.getPaymentType(), existingProject::setPaymentType);
        updateIfPresent(requestDto.getDeadline(), existingProject::setDeadline);
        if (category != null) existingProject.setCategory(category);
        if (!subcategories.isEmpty()) existingProject.setSubcategories(subcategories);

        return projectMapper.toDto(projectRepository.save(existingProject));
    }

    @Override
    public void deleteProject(UUID id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
        projectRepository.delete(project);
    }

    private <T> void updateIfPresent(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private void updateIfPresent(String value, Consumer<String> setter) {
        if (value != null && !value.isBlank()) {
            setter.accept(HtmlUtils.htmlEscape(value.trim()));
        }
    }

    private static ProjectRequestDTO sanitizeProjectRequest(ProjectRequestDTO request) {
        if (request == null) return null;
        BigDecimal budget = request.getBudget();
        if (budget != null) {
            budget = budget.setScale(2, RoundingMode.HALF_UP);
        }
        return ProjectRequestDTO.builder()
                .customerId(request.getCustomerId())
                .freelancerId(request.getFreelancerId())
                .title(request.getTitle() != null
                        ? sanitizeOptionalText(request.getTitle().trim(), "Title")
                        : null)
                .description(request.getDescription() != null
                        ? sanitizeOptionalText(request.getDescription().trim(), "Description")
                        : null)
                .status(request.getStatus())
                .budget(budget)
                .paymentType(request.getPaymentType())
                .deadline(request.getDeadline())
                .categoryId(request.getCategoryId())
                .subcategoryIds(request.getSubcategoryIds())
                .build();
    }

    private static String sanitizeOptionalText(String input, String fieldName) {
        if (input == null) return null;
        String sanitized = SanitizationUtil.sanitizeText(input);
        if (sanitized == null) throw new IllegalArgumentException(fieldName + " contains invalid characters.");
        return sanitized;
    }

    private UUID getFreelancerId(String token, UUID freelancerId) {
        if (freelancerId == null) {
            String email = jwtService.extractUsername(token);
            User user = userService.getUserByEmail(email);
            freelancerId = freelancerProfileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Freelancer profile not found for user: " + user.getId()))
                    .getId();
        }
        return freelancerId;
    }

    private UUID getCustomerId(String token, UUID customerId) {
        if (customerId == null) {
            String email = jwtService.extractUsername(token);
            User user = userService.getUserByEmail(email);
            customerId = customerProfileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for user: " + user.getId()))
                    .getId();
        }
        return customerId;
    }

    private Role getRole(String token) {
        String roleString = jwtService.extractClaim(token, claims -> claims.get("role", String.class));

        if (roleString == null || roleString.isBlank())
            throw new RoleAccessDeniedException("Role claim is missing in token");

        try {
            return Role.valueOf(roleString.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RoleAccessDeniedException("Invalid role in token: " + roleString);
        }
    }

    private Specification<Project> buildStaffSpecification(UUID freelancerId,
                                                           ProjectStatus requestedStatus,
                                                           Long categoryId,
                                                           Long subcategoryId,
                                                           String searchTerm) {

        if (requestedStatus == null) {
            return ProjectSpecifications.filterProjects(
                    null,
                    freelancerId,
                    null,
                    categoryId,
                    subcategoryId,
                    searchTerm
            );
        }

        if (STAFF_GLOBAL_STATUSES.contains(requestedStatus)) {
            return ProjectSpecifications.filterProjects(
                    null,
                    null,
                    requestedStatus,
                    categoryId,
                    subcategoryId,
                    searchTerm
            );
        } else if (STAFF_RESTRICTED_STATUSES.contains(requestedStatus)) {
            return ProjectSpecifications.filterProjects(
                    null,
                    freelancerId,
                    requestedStatus,
                    categoryId,
                    subcategoryId,
                    searchTerm
            );
        }

        return null;
    }
}
