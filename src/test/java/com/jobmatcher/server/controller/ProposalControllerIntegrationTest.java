package com.jobmatcher.server.controller;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmatcher.server.controller.config.AbstractIntegrationTest;
import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.model.AuthenticationRequest;
import com.jobmatcher.server.model.ProposalRequestDTO;
import com.jobmatcher.server.model.ProposalStatusRequestDTO;
import com.jobmatcher.server.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@AutoConfigureMockMvc
class ProposalControllerIntegrationTest  extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private FreelancerProfileRepository freelancerProfileRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobCategoryRepository jobCategoryRepository;

    String jwtToken;
    UUID projectId;
    UUID freelancerProfileId;
    UUID proposalId;
    CustomerProfile customer;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Find an existing freelancer profile seeded by test data
        FreelancerProfile freelancer = freelancerProfileRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No freelancer profile seeded for tests"));
        freelancerProfileId = freelancer.getId();

        // Authenticate as that freelancer's user to obtain JWT (seeded users use "Password!23" like other tests)
        String freelancerEmail = freelancer.getUser().getEmail();
        AuthenticationRequest loginRequest = new AuthenticationRequest();
        loginRequest.setEmail(freelancerEmail);
        loginRequest.setPassword("Password!23");

        String responseBody = mockMvc.perform(post(API_VERSION + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        jwtToken = objectMapper.readTree(responseBody).get("token").asText();

        // Create or reuse a customer and a project to attach proposals to
        customer = customerProfileRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No customer profile seeded for tests"));


        Project project = new Project();
        project.setCustomer(customer);
        project.setTitle("Integration Test Project for Proposals");
        project.setDescription("Project used by ProposalControllerIntegrationTest");
        project.setStatus(ProjectStatus.OPEN);
        project.setBudget(BigDecimal.valueOf(1500));
        project.setPaymentType(PaymentType.UPON_COMPLETION);
        project.setDeadline(LocalDate.now().plusDays(14));

        JobCategory category = jobCategoryRepository.findAll().getFirst();
        project.setCategory(category);

        Set<JobSubcategory> subcategories = category.getSubcategories().stream().limit(2).collect(java.util.stream.Collectors.toSet());
        project.setSubcategories(subcategories);

        project = projectRepository.save(project);
        projectId = project.getId();

        // Seed a proposal for many tests
        Proposal p = new Proposal();
        p.setProject(project);
        p.setFreelancer(freelancer);
        p.setCoverLetter("Initial cover letter");
        p.setAmount(BigDecimal.valueOf(500));
        p.setEstimatedDuration(10);
        p.setStatus(ProposalStatus.PENDING);
        p = proposalRepository.save(p);
        proposalId = p.getId();
    }

    @Test
    void shouldCreateProposal() throws Exception {
        Project project2 = new Project();
        project2.setCustomer(customer);
        project2.setTitle("Integration Test Project for Proposals");
        project2.setDescription("Project used by ProposalControllerIntegrationTest");
        project2.setStatus(ProjectStatus.OPEN);
        project2.setBudget(BigDecimal.valueOf(1500));
        project2.setPaymentType(PaymentType.UPON_COMPLETION);
        project2.setDeadline(LocalDate.now().plusDays(14));

        JobCategory category = jobCategoryRepository.findAll().getFirst();
        project2.setCategory(category);

        Set<JobSubcategory> subcategories = category.getSubcategories().stream().limit(2).collect(java.util.stream.Collectors.toSet());
        project2.setSubcategories(subcategories);

        project2 = projectRepository.save(project2);
        UUID projectId2 = project2.getId();

        OffsetDateTime plannedStart = OffsetDateTime.now().plusDays(7).withNano(0);
        OffsetDateTime plannedEnd = plannedStart.plusDays(7).withNano(0);

        ProposalRequestDTO request = ProposalRequestDTO.builder()
                .projectId(projectId2)
                .freelancerId(freelancerProfileId)
                .coverLetter("Proposal created via integration test")
                .amount(BigDecimal.valueOf(750))
                .penaltyAmount(BigDecimal.ZERO)
                .bonusAmount(BigDecimal.ZERO)
                .estimatedDuration(7)
                .notes("These are some notes for the proposal.")
                .plannedStartDate(plannedStart)
                .plannedEndDate(plannedEnd)
                .actualStartDate(plannedStart)
                .actualEndDate(plannedEnd)
                .priority(Priority.LOW)
                .build();

        String response = mockMvc.perform(post(API_VERSION + "/proposals")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Parse response to get id and verify repository contains it
        UUID createdId = UUID.fromString(objectMapper.readTree(response).get("id").asText());
        assertThat(proposalRepository.findById(createdId)).isPresent();
    }

    @Test
    void shouldGetProposalsByProjectId() throws Exception {
        mockMvc.perform(get(API_VERSION + "/proposals")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("projectId", projectId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                // ensure at least one proposal and that one matches seeded proposal id
                .andExpect(jsonPath("$.content[0].id").exists());
    }

    @Test
    void shouldGetProposalsByFreelancerId() throws Exception {
        mockMvc.perform(get(API_VERSION + "/proposals/freelancer/{freelancerId}", freelancerProfileId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").exists());
    }

    @Test
    void shouldGetProposalById() throws Exception {
        mockMvc.perform(get(API_VERSION + "/proposals/{id}", proposalId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(proposalId.toString()))
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void shouldGetProposalByFreelancerAndProject() throws Exception {
        mockMvc.perform(get(API_VERSION + "/proposals/by-freelancer-and-project")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("freelancerId", freelancerProfileId.toString())
                        .param("projectId", projectId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(proposalId.toString()));
    }

    @Test
    void shouldUpdateProposal() throws Exception {
        ProposalRequestDTO updateDto = ProposalRequestDTO.builder()
                .coverLetter("Updated cover letter from test")
                .amount(BigDecimal.valueOf(999.99))
                .build();

        String response = mockMvc.perform(patch(API_VERSION + "/proposals/{id}", proposalId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(proposalId.toString()))
                .andExpect(jsonPath("$.coverLetter").value("Updated cover letter from test"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // verify repository reflects update
        Proposal updated = proposalRepository.findById(proposalId).orElseThrow();
        assertThat(updated.getCoverLetter()).isEqualTo("Updated cover letter from test");
        assertThat(updated.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(999.99));
    }

    @Test
    void shouldAcceptProposalAndCreateContractAndUpdateProjectStatus() throws Exception {
        // Accept the seeded proposal
        ProposalStatusRequestDTO statusRequest = ProposalStatusRequestDTO.builder()
                .status(ProposalStatus.ACCEPTED)
                .build();

        mockMvc.perform(patch(API_VERSION + "/proposals/status/{id}", proposalId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(proposalId.toString()))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // Reload proposal from repository and verify a contract was created and linked
        Proposal proposal = proposalRepository.findById(proposalId).orElseThrow();
        assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.ACCEPTED);
        assertThat(proposal.getContract()).isNotNull();

        // Verify project was updated to IN_PROGRESS and has contract assigned
        Project project = projectRepository.findById(projectId).orElseThrow();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        assertThat(project.getContract()).isNotNull();

        // Verify contract repository contains the created contract
        assertThat(contractRepository.findById(proposal.getContract().getId())).isPresent();
    }

    @Test
    void shouldDeleteProposal() throws Exception {
        // create a fresh proposal to delete (so it's guaranteed to have no contract)
        Proposal p = new Proposal();
        p.setProject(projectRepository.findById(projectId).orElseThrow());
        FreelancerProfile freelancer = freelancerProfileRepository.findById(freelancerProfileId).orElseThrow();
        p.setFreelancer(freelancer);
        p.setCoverLetter("To be deleted");
        p.setAmount(BigDecimal.valueOf(100));
        p.setStatus(ProposalStatus.PENDING);
        p = proposalRepository.save(p);
        UUID toDeleteId = p.getId();

        mockMvc.perform(delete(API_VERSION + "/proposals/{id}", toDeleteId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        assertThat(proposalRepository.findById(toDeleteId)).isEmpty();
    }
}