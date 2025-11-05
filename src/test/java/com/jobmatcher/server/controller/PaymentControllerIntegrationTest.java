package com.jobmatcher.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmatcher.server.controller.config.AbstractIntegrationTest;
import com.jobmatcher.server.domain.Invoice;
import com.jobmatcher.server.domain.InvoiceStatus;
import com.jobmatcher.server.model.PaymentRequestDTO;
import com.jobmatcher.server.repository.InvoiceRepository;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.stripe.exception.InvalidRequestException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class PaymentControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InvoiceRepository invoiceRepository;

    String jwtToken;
    UUID unpaidInvoiceId;
    UUID paidInvoiceId;

    @BeforeEach
    void setUp() throws Exception {
        // Authenticate seeded user
        String seededEmail = "user1@jobmatcher.com";
        String seededPassword = "Password!23";

        var loginRequest = new com.jobmatcher.server.model.AuthenticationRequest();
        loginRequest.setEmail(seededEmail);
        loginRequest.setPassword(seededPassword);

        String responseBody = mockMvc.perform(post(API_VERSION + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        jwtToken = objectMapper.readTree(responseBody).get("token").asText();

        // pick one paid and create one unpaid invoice
        Invoice anyPaid = invoiceRepository.findAll().stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PAID)
                .findFirst().orElseThrow();

        paidInvoiceId = anyPaid.getId();

        Invoice unpaid = new Invoice();
        unpaid.setAmount(BigDecimal.valueOf(100));
        unpaid.setStatus(InvoiceStatus.PENDING);
        unpaid.setContract(anyPaid.getContract());
        unpaid = invoiceRepository.save(unpaid);
        unpaidInvoiceId = unpaid.getId();
    }

    @Test
    void shouldGetPaymentByInvoiceId() throws Exception {
        mockMvc.perform(get(API_VERSION + "/payments/invoice/{invoiceId}", paidInvoiceId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoice.id").value(paidInvoiceId.toString()));
        ;
    }

    @Test
    void shouldFailToCreateCheckoutForPaidInvoice() throws Exception {
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .invoiceId(paidInvoiceId.toString())
                .build();

        mockMvc.perform(post(API_VERSION + "/payments/stripe/checkout")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invoice is already marked as PAID"));
    }

    @Test
    void shouldCreateStripeCheckoutSession() throws Exception {
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .invoiceId(unpaidInvoiceId.toString())
                .build();

        try (MockedStatic<Session> mockedSession = Mockito.mockStatic(Session.class)) {
            Session fakeSession = mock(Session.class);
            when(fakeSession.getUrl()).thenReturn("https://fake-checkout.url");

            mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(fakeSession);

            mockMvc.perform(post(API_VERSION + "/payments/stripe/checkout")
                            .header("Authorization", "Bearer " + jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").value("https://fake-checkout.url"));
        }
    }

    @Test
    void shouldHandleStripeWebhookInvalidSignature() throws Exception {
        String payload = "{}";
        String invalidSig = "invalid_signature";

        mockMvc.perform(post(API_VERSION + "/payments/stripe/webhook")
                        .content(payload)
                        .header("Stripe-Signature", invalidSig))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid signature"));
    }

    @Test
    void shouldGetAllPaymentsIntegration() throws Exception {
        mockMvc.perform(get(API_VERSION + "/payments")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].invoice.id").exists())
                .andExpect(jsonPath("$.content[0].amount").exists());
    }

    @Test
    void shouldGetPaymentByIdIntegration() throws Exception {
        UUID paymentId = invoiceRepository.findById(unpaidInvoiceId)
                .orElseThrow()
                .getContract()
                .getPayment()
                .getId();

        mockMvc.perform(get(API_VERSION + "/payments/{paymentId}", paymentId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.invoice.id").exists())
                .andExpect(jsonPath("$.amount").exists());
    }

    @Test
    void shouldHandleStripeWebhookIntegration() throws Exception {
        // Prepare fake session with clientReferenceId = unpaidInvoiceId
        Session fakeSession = mock(Session.class);
        when(fakeSession.getClientReferenceId()).thenReturn(unpaidInvoiceId.toString());
        when(fakeSession.getId()).thenReturn("cs_test_123");

        // Mock EventDataObjectDeserializer
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(fakeSession));

        // Mock Event
        Event fakeEvent = mock(Event.class);
        when(fakeEvent.getType()).thenReturn("checkout.session.completed");
        when(fakeEvent.getDataObjectDeserializer()).thenReturn(deserializer);

        // Mock Webhook.constructEvent(...)
        try (MockedStatic<Webhook> webhookMock = Mockito.mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(fakeEvent);

            String payload = """
                    {
                      "id": "evt_test_webhook",
                      "object": "event",
                      "type": "checkout.session.completed",
                      "data": {
                        "object": {
                          "id": "cs_test_123",
                          "client_reference_id": "%s"
                        }
                      }
                    }
                    """.formatted(unpaidInvoiceId);

            mockMvc.perform(post(API_VERSION + "/payments/stripe/webhook")
                            .content(payload)
                            .header("Stripe-Signature", "t=123,v1=fakesignature"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Webhook handled"));
        }

        // Reload invoice from DB
        Invoice updated = invoiceRepository.findById(unpaidInvoiceId).orElseThrow();
        assert (updated.getStatus() == InvoiceStatus.PAID);
    }

    @Test
    void shouldFallbackToJsonDeserializationInWebhook() throws Exception {
        String payload = """
                {
                  "id": "evt_test_webhook",
                  "object": "event",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_fallback_test",
                      "client_reference_id": "%s"
                    }
                  }
                }
                """.formatted(unpaidInvoiceId);

        try (MockedStatic<Webhook> webhookMock = Mockito.mockStatic(Webhook.class);
             MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {

            // Mock the Stripe Event
            Event fakeEvent = mock(Event.class);
            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(deserializer.getObject()).thenReturn(Optional.empty());
            when(fakeEvent.getDataObjectDeserializer()).thenReturn(deserializer);
            when(fakeEvent.getType()).thenReturn("checkout.session.completed");

            // Stub getData().toJson() for JSON fallback
            com.stripe.model.Event.Data fakeData = mock(com.stripe.model.Event.Data.class);
            when(fakeEvent.getData()).thenReturn(fakeData);
            when(fakeData.toJson()).thenReturn("""
                    {
                      "object": {
                        "id": "cs_fallback_test",
                        "client_reference_id": "%s"
                      }
                    }
                    """.formatted(unpaidInvoiceId));

            // Stub Session.retrieve
            Session fakeSession = mock(Session.class);
            when(fakeSession.getId()).thenReturn("cs_fallback_test");
            when(fakeSession.getClientReferenceId()).thenReturn(unpaidInvoiceId.toString());
            sessionMock.when(() -> Session.retrieve("cs_fallback_test")).thenReturn(fakeSession);

            // Stub webhook
            webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(fakeEvent);

            mockMvc.perform(post(API_VERSION + "/payments/stripe/webhook")
                            .content(payload)
                            .header("Stripe-Signature", "t=123,v1=fakesignature"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Webhook handled"));
        }

        Invoice updated = invoiceRepository.findById(unpaidInvoiceId).orElseThrow();
        assert (updated.getStatus() == InvoiceStatus.PAID);
    }


    @Test
    void shouldSkipWebhookIfClientReferenceIdMissing() throws Exception {
        String payload = """
                {
                  "id": "evt_test_webhook",
                  "object": "event",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_missing_client_ref",
                      "client_reference_id": null
                    }
                  }
                }
                """;

        try (MockedStatic<Webhook> webhookMock = Mockito.mockStatic(Webhook.class)) {
            Event fakeEvent = mock(Event.class);
            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(deserializer.getObject()).thenReturn(Optional.of(mock(Session.class)));
            when(fakeEvent.getDataObjectDeserializer()).thenReturn(deserializer);
            when(fakeEvent.getType()).thenReturn("checkout.session.completed");

            webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(fakeEvent);

            mockMvc.perform(post(API_VERSION + "/payments/stripe/webhook")
                            .content(payload)
                            .header("Stripe-Signature", "t=123,v1=fakesignature"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Webhook handled"));
        }
    }

    @Test
    void shouldFailWebhookForInvalidInvoiceUuid() throws Exception {
        String payload = """
                {
                  "id": "evt_test_webhook",
                  "object": "event",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_invalid_uuid",
                      "client_reference_id": "invalid-uuid"
                    }
                  }
                }
                """;

        try (MockedStatic<Webhook> webhookMock = Mockito.mockStatic(Webhook.class)) {
            Event fakeEvent = mock(Event.class);
            Session fakeSession = mock(Session.class);
            when(fakeSession.getId()).thenReturn("cs_invalid_uuid");
            when(fakeSession.getClientReferenceId()).thenReturn("invalid-uuid");

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(deserializer.getObject()).thenReturn(Optional.of(fakeSession));
            when(fakeEvent.getDataObjectDeserializer()).thenReturn(deserializer);
            when(fakeEvent.getType()).thenReturn("checkout.session.completed");

            webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(fakeEvent);

            mockMvc.perform(post(API_VERSION + "/payments/stripe/webhook")
                            .content(payload)
                            .header("Stripe-Signature", "t=123,v1=fakesignature"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Processing error")));
        }
    }

    @Test
    void shouldReturn404IfInvoiceNotFoundForCheckout() throws Exception {
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .invoiceId(UUID.randomUUID().toString()) // non-existent
                .build();

        mockMvc.perform(post(API_VERSION + "/payments/stripe/checkout")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Invoice not found"))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldReturn500ForStripeExceptionDuringCheckout() throws Exception {
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .invoiceId(unpaidInvoiceId.toString())
                .build();

        try (MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {
            sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenThrow(new InvalidRequestException(
                            "stripe error",          // message
                            null,                    // param
                            null,                     // requestId
                            null,                     // code
                            400,                   // statusCode
                            null                    // cause
                    ));

            mockMvc.perform(post(API_VERSION + "/payments/stripe/checkout")
                            .header("Authorization", "Bearer " + jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Stripe error")));
        }
    }

    @Test
    void shouldReturn500ForUnexpectedExceptionDuringCheckout() throws Exception {
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .invoiceId(unpaidInvoiceId.toString())
                .build();

        try (MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {
            sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenThrow(new RuntimeException("unexpected error"));

            mockMvc.perform(post(API_VERSION + "/payments/stripe/checkout")
                            .header("Authorization", "Bearer " + jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Unexpected error")));
        }
    }


}