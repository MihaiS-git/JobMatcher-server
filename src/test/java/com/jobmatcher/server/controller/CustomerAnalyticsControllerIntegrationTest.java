package com.jobmatcher.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmatcher.server.controller.config.AbstractIntegrationTest;
import com.jobmatcher.server.domain.Contract;
import com.jobmatcher.server.domain.CustomerProfile;
import com.jobmatcher.server.domain.Invoice;
import com.jobmatcher.server.domain.InvoiceStatus;
import com.jobmatcher.server.repository.CustomerAnalyticsRepository;
import com.jobmatcher.server.repository.CustomerProfileRepository;
import com.jobmatcher.server.repository.ContractRepository;
import com.jobmatcher.server.repository.InvoiceRepository;
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
class CustomerAnalyticsControllerIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerProfileRepository customerRepository;

    private String jwtToken;
    private CustomerProfile seededCustomer;
    private UUID customerId;

    @BeforeEach
    void setUp() throws Exception {
        // Authenticate seeded customer user
        var loginRequest = new com.jobmatcher.server.model.AuthenticationRequest();
        loginRequest.setEmail("user4@jobmatcher.com"); // matches seeded data
        loginRequest.setPassword("Password!23");

        String responseBody = mockMvc.perform(
                        post(API_VERSION + "/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest))
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        jwtToken = objectMapper.readTree(responseBody).get("token").asText();

        // Fetch the already seeded customer
        seededCustomer = customerRepository.findAll().stream()
                .filter(c -> c.getUser().getEmail().equals("user4@jobmatcher.com"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seeded customer not found"));

        customerId = seededCustomer.getId();
    }

    @Test
    void shouldGetMonthlySpending() throws Exception {
        mockMvc.perform(get(API_VERSION + "/customers/{customerId}/analytics/monthly-spending", customerId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].year").exists())
                .andExpect(jsonPath("$[0].month").exists())
                .andExpect(jsonPath("$[0].total").exists());
    }

    @Test
    void shouldGetProjectStats() throws Exception {
        mockMvc.perform(get(API_VERSION + "/customers/{customerId}/analytics/project-stats", customerId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").exists())
                .andExpect(jsonPath("$.active").exists());
    }

    @Test
    void shouldGetTopFreelancers() throws Exception {
        mockMvc.perform(get(API_VERSION + "/customers/{customerId}/analytics/top-freelancers", customerId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].freelancerName").exists())
                .andExpect(jsonPath("$[0].totalEarned").exists());
    }
}