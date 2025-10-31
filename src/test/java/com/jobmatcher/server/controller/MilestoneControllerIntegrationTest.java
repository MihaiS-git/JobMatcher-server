package com.jobmatcher.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmatcher.server.controller.config.AbstractIntegrationTest;
import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.model.AuthenticationRequest;
import com.jobmatcher.server.model.MilestoneRequestDTO;
import com.jobmatcher.server.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class MilestoneControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JobCategoryRepository jobCategoryRepository;

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Autowired
    private ContractRepository contractRepository;


    String jwtToken;
    UUID milestoneId;
    UUID contractId;

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
                .andReturn()
                .getResponse()
                .getContentAsString();

        jwtToken = objectMapper.readTree(responseBody).get("token").asText();

        // Use seeded Contract
        Contract contract = contractRepository.findAll().get(0);
        contractId = contract.getId();

        // Seed a Milestone
        Milestone milestone = new Milestone();
        milestone.setContract(contract);
        milestone.setTitle("Test Milestone");
        milestone.setDescription("Integration test milestone");
        milestone.setAmount(BigDecimal.valueOf(100));
        milestone.setPenaltyAmount(BigDecimal.ZERO);
        milestone.setBonusAmount(BigDecimal.ZERO);
        milestone.setEstimatedDuration(5);
        milestone.setPlannedStartDate(LocalDate.now());
        milestone.setStatus(MilestoneStatus.PENDING);
        milestone.setPriority(Priority.MEDIUM);

        milestone = milestoneRepository.save(milestone);
        milestoneId = milestone.getId();
    }

    @Test
    void shouldGetAllMilestonesByContractId() throws Exception {
        mockMvc.perform(get(API_VERSION + "/milestones")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("contractId", contractId.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(milestoneId.toString()))
                .andExpect(jsonPath("$.content[0].title").value("Test Milestone"));
    }

    @Test
    void shouldGetMilestoneById() throws Exception {
        mockMvc.perform(get(API_VERSION + "/milestones/{id}", milestoneId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(milestoneId.toString()))
                .andExpect(jsonPath("$.title").value("Test Milestone"));
    }

    @Test
    void shouldCreateMilestone() throws Exception {
        Project newProject = new Project();
        newProject.setTitle("New Project for Milestone Creation");
        newProject.setDescription("Project Description");
        newProject.setCategory(jobCategoryRepository.findAll().getFirst());
        newProject.setCustomer(customerProfileRepository.findAll().getFirst());
        newProject = projectRepository.save(newProject);

        Contract newContract = new Contract();
        newContract.setProject(newProject);
        newContract.setStartDate(OffsetDateTime.now());
        newContract.setEndDate(OffsetDateTime.now().plusMonths(1));
        newContract = contractRepository.save(newContract);
        UUID newContractId = newContract.getId();

        MilestoneRequestDTO request = MilestoneRequestDTO.builder()
                .contractId(newContractId)
                .title("New Milestone")
                .description("Created via integration test")
                .amount(BigDecimal.valueOf(200))
                .penaltyAmount(BigDecimal.ZERO)
                .bonusAmount(BigDecimal.ZERO)
                .estimatedDuration(3)
                .status(MilestoneStatus.PENDING)
                .priority(Priority.LOW)
                .plannedStartDate(LocalDate.now())
                .build();

        mockMvc.perform(post(API_VERSION + "/milestones")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Milestone"))
                .andExpect(jsonPath("$.contractId").value(newContractId.toString()));
    }

    @Test
    void shouldUpdateMilestone() throws Exception {
        MilestoneRequestDTO updateRequest = MilestoneRequestDTO.builder()
                .title("Updated Milestone")
                .description("Updated via integration test")
                .amount(BigDecimal.valueOf(150))
                .build();

        mockMvc.perform(patch(API_VERSION + "/milestones/{id}", milestoneId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Milestone"))
                .andExpect(jsonPath("$.amount").value(150));
    }

    @Test
    void shouldDeleteMilestone() throws Exception {
        // Create a separate milestone to delete
        Milestone milestoneToDelete = new Milestone();
        milestoneToDelete.setContract(contractRepository.findById(contractId).orElseThrow());
        milestoneToDelete.setTitle("To Be Deleted");
        milestoneToDelete.setDescription("Delete me");
        milestoneToDelete.setAmount(BigDecimal.valueOf(50));
        milestoneToDelete.setEstimatedDuration(1);
        milestoneToDelete.setPlannedStartDate(LocalDate.now());
        milestoneToDelete.setStatus(MilestoneStatus.PENDING);
        milestoneToDelete.setPriority(Priority.LOW);
        UUID deleteId = milestoneRepository.save(milestoneToDelete).getId();

        mockMvc.perform(delete(API_VERSION + "/milestones/{id}", deleteId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }


}