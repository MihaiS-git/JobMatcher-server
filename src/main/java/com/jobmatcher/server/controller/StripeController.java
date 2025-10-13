//package com.jobmatcher.server.controller;
//
//import com.jobmatcher.server.model.Stripe.PaymentIntentRequest;
//import com.jobmatcher.server.model.Stripe.PaymentIntentResponse;
//import com.jobmatcher.server.model.Stripe.PaymentResponse;
//import com.jobmatcher.server.service.StripeService;
//import com.stripe.exception.SignatureVerificationException;
//import com.stripe.exception.StripeException;
//import com.stripe.model.Event;
//import com.stripe.net.Webhook;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.math.BigDecimal;
//
//import static com.jobmatcher.server.model.ApiConstants.API_VERSION;
//
//@Slf4j
//@RestController
//@RequestMapping(API_VERSION + "/payments")
//public class StripeController {
//    @Value("${stripe.webhook.secret}")
//    private String STRIPE_WEBHOOK_SECRET;
//
//    private final StripeService stripeService;
//
//    public StripeController(StripeService stripeService) {
//        this.stripeService = stripeService;
//    }
//
//    /**
//     * Create a payment intent
//     */
//    @PostMapping("/create-payment-intent")
//    public ResponseEntity<?> createPaymentIntent(@RequestBody PaymentIntentRequest request) {
//        try {
//            // Validate request
//            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
//                return ResponseEntity.badRequest()
//                        .body(new PaymentResponse(false, "Invalid amount", null));
//            }
//
//            PaymentIntentResponse response = stripeService.createPaymentIntent(request);
//            return ResponseEntity.ok(response);
//
//        } catch (StripeException e) {
//            log.error("Stripe error: {}", e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(new PaymentResponse(false, "Payment processing error: " + e.getMessage(), null));
//        }
//    }
//
//    /**
//     * Retrieve payment intent status
//     */
//    @GetMapping("/payment-intent/{id}")
//    public ResponseEntity<?> getPaymentIntent(@PathVariable String id) {
//        try {
//            var paymentIntent = stripeService.retrievePaymentIntent(id);
//
//            PaymentIntentResponse response = new PaymentIntentResponse(
//                    null, // Don't expose client secret
//                    paymentIntent.getId(),
//                    paymentIntent.getAmount(),
//                    paymentIntent.getStatus()
//            );
//
//            return ResponseEntity.ok(response);
//
//        } catch (StripeException e) {
//            log.error("Error retrieving payment intent: {}", e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body(new PaymentResponse(false, "Payment intent not found", null));
//        }
//    }
//
//    /**
//     * Cancel a payment intent
//     */
//    @PostMapping("/cancel-payment-intent/{id}")
//    public ResponseEntity<?> cancelPaymentIntent(@PathVariable String id) {
//        try {
//            var paymentIntent = stripeService.cancelPaymentIntent(id);
//            return ResponseEntity.ok(
//                    new PaymentResponse(true, "Payment cancelled", paymentIntent.getId())
//            );
//        } catch (StripeException e) {
//            log.error("Error cancelling payment: {}", e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                    .body(new PaymentResponse(false, "Cannot cancel payment", null));
//        }
//    }
//
//    /**
//     * Create a refund
//     */
//    @PostMapping("/refund")
//    public ResponseEntity<?> createRefund(
//            @RequestParam String paymentIntentId,
//            @RequestParam(required = false) BigDecimal amount) {
//        try {
//            var refund = stripeService.createRefund(paymentIntentId, amount);
//            return ResponseEntity.ok(
//                    new PaymentResponse(true, "Refund processed", refund.getId())
//            );
//        } catch (StripeException e) {
//            log.error("Error creating refund: {}", e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                    .body(new PaymentResponse(false, "Refund failed: " + e.getMessage(), null));
//        }
//    }
//
//    /**
//     * Webhook endpoint - This is where Stripe sends event notifications
//     * CRITICAL: Webhook signature verification for security
//     */
//    @PostMapping("/webhook")
//    public ResponseEntity<String> handleWebhook(
//            @RequestBody String payload,
//            @RequestHeader("Stripe-Signature") String sigHeader) {
//
//        Event event;
//
//        try {
//            // Verify webhook signature - CRITICAL for security
//            // This ensures the webhook actually came from Stripe
//            event = Webhook.constructEvent(payload, sigHeader, STRIPE_WEBHOOK_SECRET);
//            log.info("Received Stripe webhook event: {}", event.getType());
//            log.info("Webhook payload: {}", payload);
//
//        } catch (SignatureVerificationException e) {
//            log.error("Invalid webhook signature: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
//        } catch (Exception e) {
//            log.error("Webhook error: {}", e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook error");
//        }
//
//        // Process the event
//        stripeService.processWebhookEvent(event);
//
//        // Return 200 to acknowledge receipt
//        return ResponseEntity.ok("Webhook processed");
//    }
//}
