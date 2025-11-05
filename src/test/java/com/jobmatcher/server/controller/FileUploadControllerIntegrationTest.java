package com.jobmatcher.server.controller;

import com.jobmatcher.server.controller.config.AbstractIntegrationTest;
import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.model.AuthenticationRequest;
import com.jobmatcher.server.repository.UserRepository;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmatcher.server.service.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class FileUploadControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private UserRepository userRepository;

    UUID userId;
    String jwtToken;
    User user;
    static boolean uploaded;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        CloudinaryService testCloudinaryService() {
            return new CloudinaryService(null, null, null, null, null) {
                @Override
                public void uploadImage(UUID id, org.springframework.web.multipart.MultipartFile file) {
                    // mark that upload was called
                    uploaded = true;
                }
            };
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        uploaded = false;

        // Authenticate seeded user
        String seededEmail = "user4@jobmatcher.com";
        String seededPassword = "Password!23";

        var loginRequest = new AuthenticationRequest();
        loginRequest.setEmail(seededEmail);
        loginRequest.setPassword(seededPassword);

        String responseBody = mockMvc.perform(post(API_VERSION + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        user = userRepository.findByEmail(seededEmail).orElseThrow();
        userId = user.getId();

        jwtToken = objectMapper.readTree(responseBody).get("token").asText();
    }

    @Test
    void shouldUploadProfilePictureSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "dummy image content".getBytes()
        );

        mockMvc.perform(multipart(API_VERSION + "/users/{id}/profile_picture", userId)
                        .file(file)
                        .header("Authorization", "Bearer " + jwtToken)
                        .with(request -> {
                            request.setMethod("PATCH"); // multipart defaults to POST
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true));

        assertThat(uploaded).isTrue(); // verify the test CloudinaryService was called
    }
}