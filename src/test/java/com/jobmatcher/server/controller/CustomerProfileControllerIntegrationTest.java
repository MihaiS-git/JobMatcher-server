package com.jobmatcher.server.controller;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmatcher.server.controller.config.AbstractIntegrationTest;
import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.model.*;
import com.jobmatcher.server.repository.CustomerProfileRepository;
import com.jobmatcher.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@AutoConfigureMockMvc
class CustomerProfileControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    String jwtToken;
    User user;
    UUID customerProfileId;

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

        customerProfileId = customerProfileRepository.findByUserId(user.getId())
                .orElseThrow().getId();
    }

    @Test
    void shouldGetCustomerProfileById() throws Exception {
        mockMvc.perform(get(API_VERSION + "/profiles/customers/{id}", customerProfileId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileId").value(customerProfileId.toString()))
                .andExpect(jsonPath("$.userId").value(user.getId().toString()));
    }

    @Test
    void shouldGetCustomerProfileByUserId() throws Exception {
        mockMvc.perform(get(API_VERSION + "/profiles/customers/users/{userId}", user.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileId").value(customerProfileId.toString()))
                .andExpect(jsonPath("$.userId").value(user.getId().toString()));
    }

    @Test
    void shouldCreateCustomerProfile() throws Exception {
        CustomerProfileRequestDTO request = CustomerProfileRequestDTO.builder()
                .userId(user.getId())
                .username("testcustomer")
                .about("This is a test customer profile.")
                .company("Test Company")
                .websiteUrl("https://example.com")
                .build();

        String response = mockMvc.perform(post(API_VERSION + "/profiles/customers")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.company").value("Test Company"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        CustomerDetailDTO created = objectMapper.readValue(response, CustomerDetailDTO.class);
        customerProfileRepository.findById(created.getProfileId())
                .orElseThrow(() -> new AssertionError("Profile was not persisted in DB"));
    }

    @Test
    void shouldUpdateCustomerProfileById() throws Exception {
        CustomerProfileRequestDTO updateRequest = CustomerProfileRequestDTO.builder()
                .userId(user.getId())
                .username("updatedusername")
                .about("Updated about section")
                .company("Updated Company")
                .websiteUrl("https://updated-example.com")
                .build();

        mockMvc.perform(patch(API_VERSION + "/profiles/customers/update/{id}", customerProfileId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileId").value(customerProfileId.toString()))
                .andExpect(jsonPath("$.company").value("Updated Company"))
                .andExpect(jsonPath("$.websiteUrl").value("https://updated-example.com"));
    }

}