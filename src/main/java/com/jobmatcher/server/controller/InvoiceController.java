package com.jobmatcher.server.controller;

import com.jobmatcher.server.model.InvoiceDetailDTO;
import com.jobmatcher.server.model.InvoiceFilterDTO;
import com.jobmatcher.server.model.InvoiceRequestDTO;
import com.jobmatcher.server.model.InvoiceSummaryDTO;
import com.jobmatcher.server.service.IInvoiceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;

@RestController
@RequestMapping(API_VERSION + "/invoices")
public class InvoiceController {

    private final IInvoiceService invoiceService;

    public InvoiceController(IInvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public ResponseEntity<Page<InvoiceSummaryDTO>> getAllInvoicesByProfileId(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String profileId,
            @ModelAttribute InvoiceFilterDTO filter,
            Pageable pageable
    ) {
        Page<InvoiceSummaryDTO> page = invoiceService.getAllInvoicesByProfileId(
                authHeader,
                profileId,
                filter,
                pageable
        );
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{invoiceId}")
    public ResponseEntity<InvoiceDetailDTO> getInvoiceById(@RequestParam String invoiceId) {
        InvoiceDetailDTO invoice = invoiceService.getInvoiceById(UUID.fromString(invoiceId));
        return ResponseEntity.ok(invoice);
    }

    @PostMapping
    public ResponseEntity<InvoiceDetailDTO> createInvoice(@RequestBody InvoiceRequestDTO request) {
        InvoiceDetailDTO invoice = invoiceService.createInvoice(request);
        return ResponseEntity.ok(invoice);
    }

    @DeleteMapping("/{invoiceId}")
    public ResponseEntity<Void> deleteInvoiceById(@PathVariable String invoiceId) {
        invoiceService.deleteInvoice(UUID.fromString(invoiceId));
        return ResponseEntity.noContent().build();
    }
}
