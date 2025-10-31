package com.jobmatcher.server.controller;

import com.jobmatcher.server.controller.config.AbstractIntegrationTest;
import com.jobmatcher.server.domain.PasswordResetToken;
import com.jobmatcher.server.domain.Role;
import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.repository.PasswordResetTokenRepository;
import com.jobmatcher.server.repository.UserRepository;
import com.jobmatcher.server.service.GmailSender;
import com.jobmatcher.server.service.IRefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmatcher.server.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
public class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IRefreshTokenService refreshTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @TestConfiguration
    static class NoOpEmailConfig {
        @Bean
        public GmailSender gmailSender() {
            return new GmailSender(null) {
                @Override
                public void sendResetEmail(User user, String token) {
                    // no-op, simulate success
                }
            };
        }
    }

    @BeforeEach
    void setUp() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("existing-valid-reset-token");
        token.setUser(userRepository.findByEmail("user1@jobmatcher.com").get());
        token.setExpiryDate(LocalDateTime.now().plusMinutes(30));
        tokenRepository.save(token);
    }


    // -----------------------
    // Registration
    // -----------------------
    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest();
                request.setEmail("newuser@jobmatcher.com");
                request.setPassword("Password!23");
                request.setFirstName("Alice");
                request.setLastName("Smith");
                request.setRole(Role.CUSTOMER);

        mockMvc.perform(post("/api/v0/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldFailRegistrationWhenEmailExists() throws Exception {
        RegisterRequest request = new RegisterRequest();
                request.setEmail("user1@jobmatcher.com");
                request.setPassword("Password!23");
                request.setFirstName("Alice");
                request.setLastName("Smith");
                request.setRole(Role.CUSTOMER);

        mockMvc.perform(post("/api/v0/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    // -----------------------
    // Login
    // -----------------------
    @Test
    void shouldLoginSuccessfully() throws Exception {
        AuthenticationRequest request = new AuthenticationRequest();
                request.setEmail("user1@jobmatcher.com");
                request.setPassword("Password!23");

        mockMvc.perform(post("/api/v0/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.email").value("user1@jobmatcher.com"));
    }

    @Test
    void shouldFailLoginWithInvalidPassword() throws Exception {
        AuthenticationRequest request = new AuthenticationRequest();
                request.setEmail("user1@jobmatcher.com");
                request.setPassword("WrongPassword");

        mockMvc.perform(post("/api/v0/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    // -----------------------
    // Password recovery
    // -----------------------
    @Test
    void shouldRecoverPasswordSuccessfully() throws Exception {
        RecoverPasswordRequest request = new RecoverPasswordRequest();
        request.setEmail("user1@jobmatcher.com");

        mockMvc.perform(post("/api/v0/auth/recover-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldFailRecoverPasswordForUnknownEmail() throws Exception {
        RecoverPasswordRequest request = new RecoverPasswordRequest();
        request.setEmail("unknown@jobmatcher.com");

        mockMvc.perform(post("/api/v0/auth/recover-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    // -----------------------
    // Refresh token
    // -----------------------
    @Test
    void shouldRefreshTokenSuccessfully() throws Exception {
        // Login
        AuthenticationRequest loginRequest = new AuthenticationRequest();
        loginRequest.setEmail("user1@jobmatcher.com");
        loginRequest.setPassword("Password!23");

        String responseBody = mockMvc.perform(post("/api/v0/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();

        String refreshToken = objectMapper.readTree(responseBody).get("refreshToken").asText();
        System.out.println("Obtained Refresh Token: " + refreshToken);

        // Refresh token request
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(refreshToken);

        mockMvc.perform(post("/api/v0/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void shouldValidateResetTokenSuccessfully() throws Exception {
        // Given: a token that exists in DB (assuming test seed contains one)
        String validToken = "existing-valid-reset-token";

        mockMvc.perform(get("/api/v0/auth/validate-reset-token")
                        .param("token", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token is valid."));
    }

    @Test
    void shouldReturnErrorForInvalidResetToken() throws Exception {
        mockMvc.perform(get("/api/v0/auth/validate-reset-token")
                        .param("token", "non-existent-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_INVALID"))
                .andExpect(jsonPath("$.message").value("Invalid or expired reset token."));
    }

    @Test
    void shouldResetPasswordSuccessfully() throws Exception {
        // Assuming valid token exists
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("existing-valid-reset-token");
        request.setPassword("newSecurePassword123!");

        mockMvc.perform(put("/api/v0/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldFailResetPasswordWithInvalidToken() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("invalid-token");
        request.setPassword("whatever");

        mockMvc.perform(put("/api/v0/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Invalid token"));
    }

    @Test
    void shouldReturnCurrentUserDetails() throws Exception {
        User user = new User();
        user.setEmail("user1@jobmatcher.com");
        user.setRole(Role.STAFF);
        user.setEnabled(true);

        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/api/v0/auth/me").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user1@jobmatcher.com"))
                .andExpect(jsonPath("$.role").value("STAFF")); // match domain role
    }

    @Test
    void shouldRejectUnauthenticatedAccessToMe() throws Exception {
        mockMvc.perform(get("/api/v0/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_INVALID"))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"));
    }
}
