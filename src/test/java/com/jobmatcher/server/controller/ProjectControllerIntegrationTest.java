package com.jobmatcher.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmatcher.server.controller.config.AbstractIntegrationTest;
import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.model.AuthenticationRequest;
import com.jobmatcher.server.model.ProjectRequestDTO;
import com.jobmatcher.server.model.ProjectStatusUpdateDTO;
import com.jobmatcher.server.repository.CustomerProfileRepository;
import com.jobmatcher.server.repository.JobCategoryRepository;
import com.jobmatcher.server.repository.ProjectRepository;
import com.jobmatcher.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class ProjectControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JobCategoryRepository jobCategoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    String jwtToken;
    String refreshToken;
    User user;
    UUID customerProfileId;
    UUID projectId;

    @BeforeEach
    void setUp() throws Exception {
        // Authenticate seeded user and get JWT
        String seededEmail = "user4@jobmatcher.com";
        String seededPassword = "Password!23";

        AuthenticationRequest loginRequest = new AuthenticationRequest();
        loginRequest.setEmail(seededEmail);
        loginRequest.setPassword(seededPassword);

        String responseBody = mockMvc.perform(post(API_VERSION + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        jwtToken = objectMapper.readTree(responseBody).get("token").asText();
        refreshToken = objectMapper.readTree(responseBody).get("refreshToken").asText();
        user = userRepository.findByEmail(seededEmail).orElseThrow();

        // Use seeded CustomerProfile
        CustomerProfile customerProfile = customerProfileRepository.findByUserId(user.getId()).orElseThrow();
        customerProfileId = customerProfile.getId();

        // Seed one project
        Project project = new Project();
        project.setCustomer(customerProfile);
        project.setTitle("Test Project");
        project.setDescription("This is a test project.");
        project.setStatus(ProjectStatus.OPEN);
        project.setBudget(BigDecimal.valueOf(5000.00));
        project.setPaymentType(PaymentType.UPON_COMPLETION);
        project.setDeadline(java.time.LocalDate.now().plusMonths(1));

        JobCategory category = jobCategoryRepository.findAll().getFirst();
        project.setCategory(category);

        Set<JobSubcategory> subcategories = category.getSubcategories().stream().limit(3).collect(Collectors.toSet());
        project.setSubcategories(subcategories);

        project = projectRepository.save(project);
        projectId = project.getId();
    }

    @Test
    void shouldGetAllProjects() throws Exception {
        mockMvc.perform(get(API_VERSION + "/projects")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(projectId.toString()));
    }

    @Test
    void shouldGetProjectById() throws Exception {
        mockMvc.perform(get(API_VERSION + "/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId.toString()))
                .andExpect(jsonPath("$.title").value("Test Project"));
    }

    @Test
    void shouldCreateProject() throws Exception {
        ProjectRequestDTO request = ProjectRequestDTO.builder()
                .customerId(customerProfileId)
                .title("New Integration Project")
                .description("Created via integration test")
                .deadline(java.time.LocalDate.now().plusDays(10))
                .categoryId(jobCategoryRepository.findAll().getFirst().getId())
                .subcategoryIds(jobCategoryRepository.findAll().getFirst().getSubcategories().stream().limit(5)
                        .map(JobSubcategory::getId).collect(Collectors.toSet()))
                .build();

        mockMvc.perform(post(API_VERSION + "/projects")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Integration Project"));
    }

    @Test
    void shouldUpdateProjectStatus() throws Exception {
        ProjectStatusUpdateDTO statusUpdate = ProjectStatusUpdateDTO.builder()
                .status(ProjectStatus.COMPLETED)
                .build();

        mockMvc.perform(patch(API_VERSION + "/projects/status/{id}", projectId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void shouldDeleteProject() throws Exception {
        // create a separate project to delete
        Project project = new Project();
        project.setCustomer(customerProfileRepository.findById(customerProfileId).orElseThrow());
        project.setTitle("To Be Deleted");
        project.setDescription("Delete me");
        project.setStatus(ProjectStatus.OPEN);
        project.setBudget(java.math.BigDecimal.valueOf(1000));
        project.setPaymentType(PaymentType.UPON_COMPLETION);
        project.setDeadline(java.time.LocalDate.now().plusDays(5));
        project.setCategory(jobCategoryRepository.findAll().getFirst());
        project.setSubcategories(new HashSet<>(jobCategoryRepository.findAll().getFirst().getSubcategories()));
        UUID projectToDeleteId = projectRepository.save(project).getId();

        mockMvc.perform(delete(API_VERSION + "/projects/{id}", projectToDeleteId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldUpdateProjectSuccessfully() throws Exception {
        ProjectRequestDTO dto = ProjectRequestDTO.builder()
                .title("Updated Project Title")
                .description("Updated description")
                .budget(new BigDecimal("1234.56"))
                .deadline(LocalDate.now().plusDays(10))
                .categoryId(1L)
                .subcategoryIds(Set.of(1L, 2L))
                .build();

        mockMvc.perform(patch(API_VERSION + "/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Project Title"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.budget").value(1234.56));
    }

    @Test
    void shouldFailUpdateProjectWithInvalidDeadline() throws Exception {
        ProjectRequestDTO dto = ProjectRequestDTO.builder()
                .title("Bad Project")
                .deadline(LocalDate.now().minusDays(1)) // invalid
                .subcategoryIds(Set.of(1L))
                .categoryId(1L)
                .build();

        mockMvc.perform(patch(API_VERSION + "/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFetchJobFeedProjectsNoFilters() throws Exception {
        mockMvc.perform(get("/api/v0/projects/job-feed")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void shouldFetchJobFeedProjectsWithFilters() throws Exception {
        mockMvc.perform(get(API_VERSION + "/projects/job-feed")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("status", "OPEN")
                        .param("categoryId", "1")
                        .param("subcategoryId", "2")
                        .param("searchTerm", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void shouldPaginateJobFeedProjects() throws Exception {
        mockMvc.perform(get(API_VERSION + "/projects/job-feed")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void shouldReturn400ForInvalidStatusOnAllProjects() throws Exception {
        mockMvc.perform(get("/api/v0/projects")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid project status: INVALID_STATUS"));
    }

    @Test
    void shouldFallbackToAllowedStatusesForInvalidStatusOnJobFeed() throws Exception {
        mockMvc.perform(get("/api/v0/projects/job-feed")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("status", "INVALID_STATUS")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void shouldFallbackToAllowedStatusesForDisallowedStatusOnJobFeed() throws Exception {
        mockMvc.perform(get("/api/v0/projects/job-feed")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("status", "COMPLETED")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

}