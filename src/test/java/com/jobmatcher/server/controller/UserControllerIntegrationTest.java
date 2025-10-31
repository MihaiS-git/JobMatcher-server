package com.jobmatcher.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmatcher.server.controller.config.AbstractIntegrationTest;
import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.model.AddressRequestDTO;
import com.jobmatcher.server.model.UserRequestDTO;
import com.jobmatcher.server.repository.AddressRepository;
import com.jobmatcher.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
public class UserControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AddressRepository addressRepository;

    private User savedUser;

    @BeforeEach
    void setup() {
        savedUser = userRepository.findByEmail("user1@jobmatcher.com")
                .orElseThrow(() -> new RuntimeException("Test user not found in database."));
    }

    @Test
    @WithMockUser(username = "test", roles = {"STAFF"})
    void shouldGetUserById() throws Exception {
        mockMvc.perform(get("/api/v0/users/{id}", savedUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUser.getId().toString()))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    @WithMockUser(username = "test", roles = {"STAFF"})
    void shouldReturnNotFoundForInvalidUserId() throws Exception {
        mockMvc.perform(get("/api/v0/users/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @WithMockUser(username = "test", roles = {"STAFF"})
    void shouldUpdateUserFields() throws Exception {
        UserRequestDTO updateRequest = UserRequestDTO.builder()
                .firstName("Jane")
                .enabled(false)
                .build();

        mockMvc.perform(patch("/api/v0/users/update/{id}", savedUser.getId())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @WithMockUser(username = "test", roles = {"STAFF"})
    void shouldUpdateUserAddress() throws Exception {
        AddressRequestDTO addressRequest = AddressRequestDTO.builder()
                .street("123 Main St")
                .city("Springfield")
                .country("USA")
                .build();

        mockMvc.perform(patch("/api/v0/users/update/{id}/address", savedUser.getId())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(addressRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressResponseDto.street").value("123 Main St"))
                .andExpect(jsonPath("$.addressResponseDto.city").value("Springfield"))
                .andExpect(jsonPath("$.addressResponseDto.country").value("USA"));
    }

    @Test
    @WithMockUser(username = "test", roles = {"STAFF"})
    void shouldReturnNotFoundWhenUpdatingNonexistentUser() throws Exception {
        UserRequestDTO updateRequest = UserRequestDTO.builder()
                .firstName("NoOne")
                .build();

        mockMvc.perform(patch("/api/v0/users/update/{id}", UUID.randomUUID())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }
}
