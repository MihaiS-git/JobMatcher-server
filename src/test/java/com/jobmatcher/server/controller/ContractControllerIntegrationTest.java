package com.jobmatcher.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmatcher.server.controller.config.AbstractIntegrationTest;
import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.model.*;
import com.jobmatcher.server.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@AutoConfigureMockMvc
class ContractControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JobCategoryRepository jobCategoryRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @Autowired
    private FreelancerProfileRepository freelancerProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProposalRepository proposalRepository;

    String jwtToken;
    User user;
    UUID customerProfileId;
    UUID freelancerProfileId;
    UUID projectId;
    UUID contractId;
    CustomerProfile customer;
    FreelancerProfile freelancer;


    @BeforeEach
    void setUp() throws Exception {
        // Authenticate seeded user
        String seededEmail = "user4@jobmatcher.com";
        String seededPassword = "Password!23";

        AuthenticationRequest loginRequest = new AuthenticationRequest();
        loginRequest.setEmail(seededEmail);
        loginRequest.setPassword(seededPassword);

        String responseBody = mockMvc.perform(post(API_VERSION + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        jwtToken = objectMapper.readTree(responseBody).get("token").asText();
        user = userRepository.findByEmail(seededEmail).orElseThrow();

        customer = customerProfileRepository.findByUserId(user.getId()).orElseThrow();
        customerProfileId = customer.getId();

        freelancer = freelancerProfileRepository.findAll().get(0);
        freelancerProfileId = freelancer.getId();

        // Seed project
        Project project = new Project();
        project.setTitle("Contract Test Project");
        project.setDescription("Project for contract integration test");
        project.setCustomer(customer);
        project.setFreelancer(freelancer);
        project.setStatus(ProjectStatus.OPEN);
        project.setBudget(BigDecimal.valueOf(5000));
        project.setPaymentType(PaymentType.UPON_COMPLETION);
        project.setDeadline(LocalDate.now().plusMonths(1));
        project.setCategory(jobCategoryRepository.findAll().getFirst());
        project.setSubcategories(project.getCategory().getSubcategories().stream().limit(3).collect(Collectors.toSet()));
        project = projectRepository.save(project);
        projectId = project.getId();

        // Seed proposal
        Proposal proposal = new Proposal();
        proposal.setProject(project);
        proposal.setFreelancer(freelancer);
        proposal.setCoverLetter("This is a test proposal.");
        proposal.setAmount(BigDecimal.valueOf(1500.00));
        proposal.setEstimatedDuration(30);
        proposal.setPlannedStartDate(OffsetDateTime.now().plusDays(1).withNano(0));
        proposal.setPlannedEndDate(OffsetDateTime.now().plusDays(31).withNano(0));
        proposal.setStatus(ProposalStatus.ACCEPTED);
        proposal = proposalRepository.save(proposal);

        // Seed contract
        Contract contract = new Contract();
        contract.setProposal(proposal);
        contract.setProject(project);
        contract.setCustomer(customer);
        contract.setFreelancer(freelancer);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setTitle("Test Contract");
        contract.setDescription("Contract Description");
        contract.setAmount(BigDecimal.valueOf(2000.00));
        contract.setStartDate(OffsetDateTime.now().plusDays(2).withNano(0));
        contract.setEndDate(OffsetDateTime.now().plusMonths(1).withNano(0));
        contract = contractRepository.save(contract);
        contractId = contract.getId();

        proposal.setContract(contract);
        proposalRepository.save(proposal);
    }

    @Test
    void shouldGetAllContracts() throws Exception {
        mockMvc.perform(get(API_VERSION + "/contracts")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(contractId.toString()));
    }

    @Test
    void shouldGetContractById() throws Exception {
        mockMvc.perform(get(API_VERSION + "/contracts/{id}", contractId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(contractId.toString()))
                .andExpect(jsonPath("$.title").value("Test Contract"));
    }

    @Test
    void shouldGetContractByProjectId() throws Exception {
        mockMvc.perform(get(API_VERSION + "/contracts/project/{projectId}", projectId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.title").value("Test Contract"));
    }

    @Test
    void shouldUpdateContractStatus() throws Exception {
        ContractStatusRequestDTO request = ContractStatusRequestDTO.builder()
                .status(ContractStatus.COMPLETED)
                .build();

        mockMvc.perform(patch(API_VERSION + "/contracts/status/{contractId}", contractId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void shouldDeleteContract() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(30);
        OffsetDateTime now = OffsetDateTime.now().plusMinutes(1);

        // Seed project
        Project project2 = new Project();
        project2.setTitle("Contract Test Project");
        project2.setDescription("Project for contract integration test");
        project2.setCustomer(customer);
        project2.setFreelancer(freelancer);
        project2.setStatus(ProjectStatus.OPEN);
        project2.setBudget(BigDecimal.valueOf(5000));
        project2.setPaymentType(PaymentType.UPON_COMPLETION);
        project2.setDeadline(futureDate);
        project2.setCategory(jobCategoryRepository.findAll().getFirst());
        project2.setSubcategories(project2.getCategory().getSubcategories().stream().limit(3).collect(Collectors.toSet()));
        project2 = projectRepository.save(project2);
        projectId = project2.getId();

        // Seed proposal
        Proposal proposal2 = new Proposal();
        proposal2.setProject(project2);
        proposal2.setFreelancer(freelancer);
        proposal2.setCoverLetter("This is a test proposal.");
        proposal2.setAmount(BigDecimal.valueOf(1500.00));
        proposal2.setEstimatedDuration(30);
        proposal2.setPlannedStartDate(now.plusDays(1).withNano(0));
        proposal2.setPlannedEndDate(now.plusDays(31).withNano(0));
        proposal2.setStatus(ProposalStatus.ACCEPTED);
        proposal2 = proposalRepository.save(proposal2);

        // Seed contract
        Contract contract2 = new Contract();
        contract2.setProposal(proposal2);
        contract2.setProject(project2);
        contract2.setCustomer(customer);
        contract2.setFreelancer(freelancer);
        contract2.setStatus(ContractStatus.ACTIVE);
        contract2.setTitle("Test Contract");
        contract2.setDescription("Contract Description");
        contract2.setAmount(BigDecimal.valueOf(1500.00));
        contract2.setStartDate(now.plusDays(2).withNano(0));
        contract2.setEndDate(now.plusDays(32).withNano(0));
        contract2 = contractRepository.save(contract2);
        UUID contractToDeleteId = contract2.getId();

        proposal2.setContract(contract2);
        proposalRepository.save(proposal2);

        project2.setProposals(new HashSet<>(List.of(proposal2)));
        project2.setAcceptedProposal(proposal2);
        project2.setFreelancer(freelancer);
        project2 = projectRepository.save(project2);

        System.out.println("Project : " + project2);
        System.out.println("Proposal : " + proposal2);
        System.out.println("Contract to delete : " + contract2);
        System.out.println("Contract to delete ID : " + contractToDeleteId);

        mockMvc.perform(delete(API_VERSION + "/contracts/{contractId}", contractToDeleteId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

}