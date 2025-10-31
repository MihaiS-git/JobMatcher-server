package com.jobmatcher.server.controller;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmatcher.server.controller.config.AbstractIntegrationTest;
import com.jobmatcher.server.domain.FreelancerProfile;
import com.jobmatcher.server.repository.FreelancerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class FreelancerAnalyticsControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FreelancerProfileRepository freelancerRepository;

    private String jwtToken;
    private FreelancerProfile seededFreelancer;
    private UUID freelancerId;

    @BeforeEach
    void setUp() throws Exception {
        // Authenticate a seeded freelancer
        var loginRequest = new com.jobmatcher.server.model.AuthenticationRequest();
        loginRequest.setEmail("user1@jobmatcher.com");
        loginRequest.setPassword("Password!23");

        String responseBody = mockMvc.perform(
                        post(API_VERSION + "/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest))
                ).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        jwtToken = objectMapper.readTree(responseBody).get("token").asText();

        // Use existing seeded freelancer
        seededFreelancer = freelancerRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No seeded freelancer found"));
        freelancerId = seededFreelancer.getId();
    }

    @Test
    void shouldGetMonthlyEarnings() throws Exception {
        mockMvc.perform(get(API_VERSION + "/freelancers/{freelancerId}/analytics/monthly-earnings", freelancerId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].year").exists())
                .andExpect(jsonPath("$[0].month").exists())
                .andExpect(jsonPath("$[0].total").exists());
    }

    @Test
    void shouldGetJobCompletionRate() throws Exception {
        mockMvc.perform(get(API_VERSION + "/freelancers/{freelancerId}/analytics/job-completion", freelancerId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").exists())
                .andExpect(jsonPath("$.total").exists())
                .andExpect(jsonPath("$.rate").exists());
    }

    @Test
    void shouldGetTopClients() throws Exception {
        mockMvc.perform(get(API_VERSION + "/freelancers/{freelancerId}/analytics/top-clients", freelancerId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clientName").exists())
                .andExpect(jsonPath("$[0].totalSpent").exists());
    }

    @Test
    void shouldGetSkillEarnings() throws Exception {
        mockMvc.perform(get(API_VERSION + "/freelancers/{freelancerId}/analytics/skill-earnings", freelancerId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].skillName").exists())
                .andExpect(jsonPath("$[0].earnings").exists());
    }

}