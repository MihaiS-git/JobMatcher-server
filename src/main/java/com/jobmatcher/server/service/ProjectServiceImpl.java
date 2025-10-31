package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.exception.InvalidProjectOperationException;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.ProjectMapper;
import com.jobmatcher.server.model.*;
import com.jobmatcher.server.repository.*;
import com.jobmatcher.server.util.SanitizationUtil;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
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

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final CustomerProfileRepository customerProfileRepository;
    private final FreelancerProfileRepository freelancerProfileRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final JobSubcategoryRepository jobSubcategoryRepository;
    private final JwtService jwtService;
    private final IUserService userService;
    private final ContractRepository contractRepository;
    private final ProposalRepository proposalRepository;

    public ProjectServiceImpl(
            ProjectRepository projectRepository,
            ProjectMapper projectMapper,
            CustomerProfileRepository customerProfileRepository,
            FreelancerProfileRepository freelancerProfileRepository,
            JobCategoryRepository jobCategoryRepository,
            JobSubcategoryRepository jobSubcategoryRepository,
            JwtService jwtService,
            IUserService userService, ContractRepository contractRepository, ProposalRepository proposalRepository
    ) {
        this.projectRepository = projectRepository;
        this.projectMapper = projectMapper;
        this.customerProfileRepository = customerProfileRepository;
        this.freelancerProfileRepository = freelancerProfileRepository;
        this.jobCategoryRepository = jobCategoryRepository;
        this.jobSubcategoryRepository = jobSubcategoryRepository;
        this.jwtService = jwtService;
        this.userService = userService;
        this.contractRepository = contractRepository;
        this.proposalRepository = proposalRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public PagedResponseDTO<ProjectSummaryDTO> getAllProjects(
            String token,
            Pageable pageable,
            ProjectStatus status,
            Long categoryId,
            Long subcategoryId,
            String searchTerm
    ) {
        User user = getUser(token);
        Role role = user.getRole();

        UUID profileId = switch (role) {
            case CUSTOMER -> getCustomerId(user.getId());
            case STAFF -> getFreelancerId(user.getId());
            default -> null;
        };

        // Step 1: fetch all matching IDs (no Pageable, no Sort)
        List<UUID> filteredIds = projectRepository.findFilteredProjectIds(
                profileId, status, categoryId, subcategoryId, searchTerm
        );

        return buildPageResponse(pageable, filteredIds);
    }

    @Override
    public ProjectDetailDTO getProjectById(UUID id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
        return projectMapper.toDto(project);
    }

    @Override
    public ProjectDetailDTO createProject(ProjectRequestDTO requestDto) {
        if (requestDto.getCustomerId() == null) {
            throw new InvalidProjectOperationException("Please create a profile before creating a project.");
        }
        CustomerProfile customer = customerProfileRepository.findById(requestDto.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + requestDto.getCustomerId()));
        if (customer.getUser().getRole() != Role.CUSTOMER) {
            throw new InvalidProjectOperationException("Only users with CUSTOMER role can create projects.");
        }


        if (requestDto.getCategoryId() == null) {
            throw new IllegalArgumentException("Category must be provided.");
        }
        JobCategory category = jobCategoryRepository.findById(requestDto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));

        if (requestDto.getSubcategoryIds() == null || requestDto.getSubcategoryIds().isEmpty()) {
            throw new IllegalArgumentException("At least one subcategory must be selected");
        }
        Set<JobSubcategory> subcategories = new HashSet<>(jobSubcategoryRepository.findAllById(requestDto.getSubcategoryIds()));

        if (requestDto.getDeadline().isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Deadline cannot be in the past");

        ProjectRequestDTO sanitizedRequest = sanitizeProjectRequest(requestDto);

        Project newProject = projectMapper.toEntity(sanitizedRequest, customer, null, category, subcategories);

        return projectMapper.toDto(projectRepository.save(newProject));
    }

    @Override
    public ProjectDetailDTO updateProject(UUID id, ProjectRequestDTO requestDto) {
        Project existingProject = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
        JobCategory category = requestDto.getCategoryId() != null
                ? jobCategoryRepository.findById(requestDto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + requestDto.getCategoryId()))
                : null;
        Set<JobSubcategory> subcategories = requestDto.getSubcategoryIds() != null
                ? new HashSet<>(jobSubcategoryRepository.findAllById(requestDto.getSubcategoryIds()))
                : new HashSet<>();

        if (requestDto.getDeadline() != null && requestDto.getDeadline().isBefore(LocalDate.now().plusDays(1))){
            throw new IllegalArgumentException("Deadline must be a future date.");
        }

        ProjectRequestDTO sanitizedRequest = sanitizeProjectRequest(requestDto);

        if (requestDto.getFreelancerId() != null) {
            FreelancerProfile freelancer = freelancerProfileRepository.findById(requestDto.getFreelancerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found with id: " + requestDto.getFreelancerId()));
            existingProject.setFreelancer(freelancer);
        } else {
            existingProject.setFreelancer(null);
        }

        if (requestDto.getContractId() != null) {
            Contract contract = contractRepository.findById(requestDto.getContractId())
                    .orElseThrow(() -> new ResourceNotFoundException("Contract not found with id: " + requestDto.getContractId()));
            existingProject.setContract(contract);
        } else {
            existingProject.setContract(null);
        }

        if (requestDto.getAcceptedProposalId() != null) {
            Proposal proposal = proposalRepository.findById(requestDto.getAcceptedProposalId())
                    .orElseThrow(() -> new ResourceNotFoundException("Proposal not found with id: " + requestDto.getAcceptedProposalId()));
            existingProject.setAcceptedProposal(proposal);
        } else {
            existingProject.setAcceptedProposal(null);
        }

        updateIfPresent(sanitizedRequest.getTitle(), existingProject::setTitle);
        updateIfPresent(sanitizedRequest.getDescription(), existingProject::setDescription);
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

    @Override
    public PagedResponseDTO<ProjectSummaryDTO> getAllJobFeedProjects(
            Pageable pageable,
            List<ProjectStatus> statuses,
            Long categoryId,
            Long subcategoryId,
            String searchTerm
    ) {
        // Step 1: fetch all matching IDs (no Pageable, no Sort)
        List<UUID> filteredIds = projectRepository.findFilteredJobFeedProjectIds(
                statuses, categoryId, subcategoryId, searchTerm
        );

        return buildPageResponse(pageable, filteredIds);
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

    private User getUser(String token) {
        String email = jwtService.extractUsername(token);
        return userService.getUserByEmail(email);
    }

    private PagedResponseDTO<ProjectSummaryDTO> buildPageResponse(Pageable pageable, List<UUID> filteredIds) {
        if (filteredIds.isEmpty()) {
            return new PagedResponseDTO<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(),
                    0, 0, true, true);
        }

        // Step 2: fetch full entities
        List<Project> projects = projectRepository.findByIdIn(filteredIds);

        // Step 3: apply sorting from Pageable
        Comparator<Project> comparator = pageable.getSort().isSorted()
                ? pageable.getSort().stream()
                .map(order -> {
                    Comparator<Project> c = switch (order.getProperty()) {
                        case "title" -> Comparator.comparing(Project::getTitle, String.CASE_INSENSITIVE_ORDER);
                        case "status" -> Comparator.comparing(Project::getStatus);
                        case "category" ->
                                Comparator.comparing(p -> p.getCategory().getName(), String.CASE_INSENSITIVE_ORDER);
                        case "deadline" ->
                                Comparator.comparing(Project::getDeadline, Comparator.nullsLast(Comparator.naturalOrder()));
                        case "paymentType" ->
                                Comparator.comparing(Project::getPaymentType, Comparator.nullsLast(Comparator.naturalOrder()));
                        case "budget" ->
                                Comparator.comparing(Project::getBudget, Comparator.nullsLast(Comparator.naturalOrder()));
                        default -> null;
                    };
                    if (c != null && order.isDescending()) c = c.reversed();
                    return c;
                })
                .filter(Objects::nonNull)
                .reduce(Comparator::thenComparing)
                .orElse(Comparator.comparing(Project::getId))
                : Comparator.comparing(Project::getLastUpdate, Comparator.nullsLast(Comparator.naturalOrder())).reversed();

        projects.sort(comparator);

        // Step 4: apply pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), projects.size());
        List<ProjectSummaryDTO> content = projects.subList(start, end).stream()
                .map(projectMapper::toSummaryDto)
                .toList();

        return new PagedResponseDTO<>(
                content,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                projects.size(),
                (int) Math.ceil((double) projects.size() / pageable.getPageSize()),
                start == 0,
                end == projects.size()
        );
    }

    @Override
    public ProjectDetailDTO updateProjectStatus(UUID projectId, ProjectStatusUpdateDTO status) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project with ID " + projectId + " not found."));
        project.setStatus(status.getStatus());
        return projectMapper.toDto(projectRepository.save(project));
    }

}
