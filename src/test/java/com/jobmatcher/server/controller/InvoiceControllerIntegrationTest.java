package com.jobmatcher.server.controller;

import com.jobmatcher.server.domain.Contract;
import com.jobmatcher.server.repository.ContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmatcher.server.controller.config.AbstractIntegrationTest;
import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.model.*;
import com.jobmatcher.server.repository.InvoiceRepository;
import com.jobmatcher.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@AutoConfigureMockMvc
class InvoiceControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private ContractRepository contractRepository;

    private String jwtToken;
    private User user;
    private UUID existingInvoiceId;
    private Contract existingContract;

    @BeforeEach
    void setUp() throws Exception {
        // Authenticate seeded user
        String email = "user4@jobmatcher.com";
        String password = "Password!23";

        AuthenticationRequest loginRequest = new AuthenticationRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        String response = mockMvc.perform(post(API_VERSION + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        jwtToken = objectMapper.readTree(response).get("token").asText();
        user = userRepository.findByEmail(email).orElseThrow();

        existingInvoiceId = invoiceRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No invoices seeded"))
                .getId();

        existingContract = contractRepository.findAll().getFirst();

        // Create a new unpaid invoice
        Contract contract = contractRepository.findAll().stream().findFirst().orElseThrow();

        InvoiceRequestDTO request = InvoiceRequestDTO.builder()
                .contractId(contract.getId())
                .build(); // no payment, so unpaid

        String invoiceResponse = mockMvc.perform(post(API_VERSION + "/invoices")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        InvoiceDetailDTO createdInvoice = objectMapper.readValue(invoiceResponse, InvoiceDetailDTO.class);
        existingInvoiceId = createdInvoice.getId();
    }

    @Test
    void shouldGetAllInvoices() throws Exception {
        mockMvc.perform(get(API_VERSION + "/invoices")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void shouldGetAllPaidInvoices() throws Exception {
        mockMvc.perform(get(API_VERSION + "/invoices?status=PAID&contractId=&searchTerm=")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void shouldThrowGetAllUnpaidInvoices() throws Exception {
        mockMvc.perform(get(API_VERSION + "/invoices?status=UNPAID&contractId=&searchTerm=")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid invoice status: UNPAID"));
    }

    @Test
    void shouldGetInvoiceById() throws Exception {
        mockMvc.perform(get(API_VERSION + "/invoices/{id}", existingInvoiceId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingInvoiceId.toString()));
    }

    @Test
    void shouldCreateInvoice() throws Exception {
        InvoiceRequestDTO request = InvoiceRequestDTO.builder()
                .contractId(existingContract.getId())
                .build();

        mockMvc.perform(post(API_VERSION + "/invoices")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contract").exists())
                .andExpect(jsonPath("$.amount").exists());
    }

    @Test
    void shouldFailToDeletePaidInvoice() throws Exception {
        UUID paidInvoiceId = invoiceRepository.findAll().stream()
                .filter(i -> i.getPayment() != null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No paid invoices available for test"))
                .getId();

        mockMvc.perform(delete(API_VERSION + "/invoices/{id}", paidInvoiceId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot delete an invoice that has been paid."));
    }


    @Test
    void shouldDeleteInvoiceById() throws Exception {
        mockMvc.perform(delete(API_VERSION + "/invoices/{id}", existingInvoiceId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

}