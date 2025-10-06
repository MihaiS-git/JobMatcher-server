package com.jobmatcher.server.controller;

import com.jobmatcher.server.model.PaymentDetailDTO;
import com.jobmatcher.server.model.PaymentFilterDTO;
import com.jobmatcher.server.model.PaymentRequestDTO;
import com.jobmatcher.server.model.PaymentSummaryDTO;
import com.jobmatcher.server.service.IPaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;

@RestController
@RequestMapping(API_VERSION + "/payments")
public class PaymentController {

    private final IPaymentService paymentService;

    public PaymentController(IPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public ResponseEntity<Page<PaymentSummaryDTO>> getAllPaymentsByProfileId(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String profileId,
            @ModelAttribute PaymentFilterDTO filter,
            Pageable pageable
    ) {
        Page<PaymentSummaryDTO> payments = paymentService.getAllPaymentsByProfileId(authHeader, profileId, filter, pageable);
        return ResponseEntity.ok(Page.empty());
    }

    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<PaymentDetailDTO> getPaymentByInvoiceId(@PathVariable String invoiceId) {
        PaymentDetailDTO payment = paymentService.getPaymentByInvoiceId(UUID.fromString(invoiceId));
        return ResponseEntity.ok(payment);
    }

    @PostMapping
    public ResponseEntity<PaymentDetailDTO> createPayment(@RequestBody PaymentRequestDTO request) {
        PaymentDetailDTO createdPayment = paymentService.createPayment(request);
        return ResponseEntity.ok(createdPayment);
    }

    @DeleteMapping("/{paymentId}")
    public ResponseEntity<Void> deletePayment(@PathVariable String paymentId) {
        paymentService.deletePayment(UUID.fromString(paymentId));
        return ResponseEntity.noContent().build();
    }
}
