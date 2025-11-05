package com.jobmatcher.server.controller;

import com.jobmatcher.server.controller.config.AbstractIntegrationTest;
import com.jobmatcher.server.domain.FreelancerProfile;
import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.model.AuthenticationRequest;
import com.jobmatcher.server.repository.FreelancerProfileRepository;
import com.jobmatcher.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmatcher.server.domain.ExperienceLevel;
import com.jobmatcher.server.model.FreelancerDetailDTO;
import com.jobmatcher.server.model.FreelancerProfileRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class FreelancerProfileControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FreelancerProfileRepository freelancerProfileRepository;

    @Autowired
    private UserRepository userRepository;

    private String jwtToken;
    private User user;
    private FreelancerProfile existingProfile;

    @BeforeEach
    void setUp() throws Exception {
        // Authenticate seeded user
        String seededEmail = "user0@jobmatcher.com";
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

        // Pick an existing freelancer profile
        existingProfile = freelancerProfileRepository.findAll().stream()
                .findFirst()
                .orElseThrow();
    }

    @Test
    void shouldGetFreelancerById() throws Exception {
        UUID profileId = existingProfile.getId();

        mockMvc.perform(get(API_VERSION + "/profiles/freelancers/{id}", profileId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileId").value(profileId.toString()))
                .andExpect(jsonPath("$.username").value(existingProfile.getUsername()));
    }

    @Test
    void shouldGetFreelancerByUserId() throws Exception {
        UUID userId = existingProfile.getUser().getId();

        mockMvc.perform(get(API_VERSION + "/profiles/freelancers/users/{userId}", userId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.username").value(existingProfile.getUsername()));
    }

    @Test
    void shouldCreateFreelancerProfile() throws Exception {
        FreelancerProfileRequestDTO request = FreelancerProfileRequestDTO.builder()
                .userId(user.getId())
                .username("newfreelancer")
                .experienceLevel(ExperienceLevel.MID)
                .hourlyRate(50.0)
                .availableForHire(true)
                .about("A new test freelancer")
                .websiteUrl("https://example.com")
                .build();

        String response = mockMvc.perform(post(API_VERSION + "/profiles/freelancers")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(request.getUsername()))
                .andExpect(jsonPath("$.hourlyRate").value(50.0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        FreelancerDetailDTO created = objectMapper.readValue(response, FreelancerDetailDTO.class);
        freelancerProfileRepository.findById(created.getProfileId())
                .orElseThrow(() -> new AssertionError("Profile was not persisted in DB"));
    }

    @Test
    void shouldUpdateFreelancerProfileById() throws Exception {
        UUID profileId = existingProfile.getId();

        FreelancerProfileRequestDTO updateRequest = FreelancerProfileRequestDTO.builder()
                .username(existingProfile.getUsername())
                .userId(user.getId())
                .headline("Updated headline")
                .hourlyRate(80.0)
                .availableForHire(false)
                .build();

        mockMvc.perform(patch(API_VERSION + "/profiles/freelancers/update/{id}", profileId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileId").value(profileId.toString()))
                .andExpect(jsonPath("$.headline").value("Updated headline"))
                .andExpect(jsonPath("$.hourlyRate").value(80.0))
                .andExpect(jsonPath("$.availableForHire").value(false));
    }

    @Test
    void shouldReturnNotFoundWhenFreelancerNotExists() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get(API_VERSION + "/profiles/freelancers/{id}", nonExistentId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }
}