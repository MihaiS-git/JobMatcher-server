package com.jobmatcher.server.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jobmatcher.server.domain.Invoice;
import com.jobmatcher.server.domain.InvoiceStatus;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.model.*;
import com.jobmatcher.server.repository.InvoiceRepository;
import com.jobmatcher.server.service.IPaymentService;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;

@Slf4j
@RestController
@RequestMapping(API_VERSION + "/payments")
public class PaymentController {

    @Value("${stripe.api.key}")
    private String STRIPE_API_KEY;

    @Value("${stripe.webhook.secret}")
    private String STRIPE_WEBHOOK_SECRET;

    @Value("${frontend.url.dev}")
    private String frontendUrl;

    private final Gson gson = new Gson();

    private final IPaymentService paymentService;
    private final InvoiceRepository invoiceRepository;

    public PaymentController(
            IPaymentService paymentService,
            InvoiceRepository invoiceRepository
    ) {
        this.paymentService = paymentService;
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping
    public ResponseEntity<Page<PaymentSummaryDTO>> getAllPayments(
            @RequestHeader("Authorization") String authHeader,
            Pageable pageable,
            @ModelAttribute PaymentFilterDTO filter
    ) {
        String token = authHeader.replace("Bearer ", "").trim();

        Page<PaymentSummaryDTO> page = paymentService.getAllPayments(
                authHeader,
                pageable,
                filter
        );
        return ResponseEntity.ok(page);
    }

    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<PaymentDetailDTO> getPaymentByInvoiceId(@PathVariable String invoiceId) {
        PaymentDetailDTO payment = paymentService.getPaymentByInvoiceId(UUID.fromString(invoiceId));
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/stripe/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        log.info("Received Stripe webhook");
        Event event;
        try {
            log.info("Verifying Stripe webhook signature");
            event = Webhook.constructEvent(payload, sigHeader, STRIPE_WEBHOOK_SECRET);
        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe webhook signature: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid signature");
        } catch (Exception e) {
            log.error("Error parsing Stripe webhook event: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid payload");
        }

        try {
            log.info("Received Stripe event: {}", event.getType());
            switch (event.getType()) {
                case "checkout.session.completed" -> {
                    log.info("Handling checkout.session.completed event");
                    handleCheckoutSessionCompleted(event);
                }
                default -> log.warn("Unhandled Stripe event type: {}", event.getType());
            }

            return ResponseEntity.ok("Webhook handled");

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Processing error");
        }
    }

    private void handleCheckoutSessionCompleted(Event event) {
        log.info("Processing checkout.session.completed event");
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        if (deserializer.getObject().isPresent() && deserializer.getObject().get() instanceof Session session) {
            log.info("Deserialized session object directly");
            processSession(session);
        } else {
            log.info("Deserializing session object from JSON");
            String sessionId = ((JsonObject) gson.fromJson(event.getData().toJson(), JsonObject.class))
                    .get("object").getAsJsonObject().get("id").getAsString();
            try {
                log.info("Retrieving session ID: {}", sessionId);
                Session session = Session.retrieve(sessionId);
                processSession(session);
            } catch (StripeException e) {
                log.error("Failed to retrieve session {}: {}", sessionId, e.getMessage());
                throw new RuntimeException(e); // fail webhook so Stripe retries
            }
        }
    }

    private void processSession(Session session) {
        log.info("Processing session ID: {}", session.getId());
        String clientReferenceId = session.getClientReferenceId();

        if (clientReferenceId == null || clientReferenceId.isBlank()) {
            log.warn("Session {} missing client_reference_id, skipping", session.getId());
            return;
        }

        try {
            UUID invoiceId = UUID.fromString(clientReferenceId.trim());
            log.info("Marking invoice {} as PAID", invoiceId);
            paymentService.markInvoicePaid(invoiceId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid invoice ID in session {}: {}", session.getId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error marking invoice as PAID for session {}: {}", session.getId(), e.getMessage());
            throw new RuntimeException(e);
        }
    }


    @PostMapping("/stripe/checkout")
    public ResponseEntity<?> createCheckoutSession(@RequestBody PaymentRequestDTO requestDTO) {

        Invoice invoice = invoiceRepository.findById(UUID.fromString(requestDTO.getInvoiceId())).orElseThrow(() ->
                new ResourceNotFoundException("Invoice not found"));
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            return ResponseEntity.badRequest().body("Invoice is already marked as PAID");
        }
        try {
            Stripe.apiKey = STRIPE_API_KEY;

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setClientReferenceId(requestDTO.getInvoiceId())
                    .setSuccessUrl(frontendUrl + "/invoice-success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(frontendUrl + "/invoice-cancel")
                    .addAllLineItem(List.of(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("usd")
                                                    .setUnitAmount(invoice.getAmount()
                                                            .multiply(BigDecimal.valueOf(100))
                                                            .longValue())
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Invoice Payment")
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    ))
                    .build();

            Session session = Session.create(params);

            return ResponseEntity.ok(new StripeCheckoutResponseDTO(session.getUrl()));
        } catch (StripeException e) {
            return ResponseEntity.internalServerError().body("Stripe error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Unexpected error: " + e.getMessage());
        }
    }

    @DeleteMapping("/{paymentId}")
    public ResponseEntity<Void> deletePayment(@PathVariable String paymentId) {
        paymentService.deletePayment(UUID.fromString(paymentId));
        return ResponseEntity.noContent().build();
    }
}
