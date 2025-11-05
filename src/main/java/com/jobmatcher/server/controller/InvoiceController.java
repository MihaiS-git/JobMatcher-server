package com.jobmatcher.server.controller;

import com.jobmatcher.server.domain.InvoiceStatus;
import com.jobmatcher.server.model.InvoiceDetailDTO;
import com.jobmatcher.server.model.InvoiceFilterDTO;
import com.jobmatcher.server.model.InvoiceRequestDTO;
import com.jobmatcher.server.model.InvoiceSummaryDTO;
import com.jobmatcher.server.service.IInvoiceService;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;

@Slf4j
@RestController
@RequestMapping(API_VERSION + "/invoices")
public class InvoiceController {

    private final IInvoiceService invoiceService;

    public InvoiceController(IInvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public ResponseEntity<Page<InvoiceSummaryDTO>> getAllInvoices(
            @RequestHeader("Authorization") String authHeader,
            @ParameterObject Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String contractId,
            @RequestParam(required = false) String searchTerm
    ) {
        String token = authHeader.replace("Bearer ", "").trim();
        log.info("Received request to get all invoices with filters - status: {}, contractId: {}, searchTerm: {}",
                status, contractId, searchTerm);
        InvoiceStatus invoiceStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                invoiceStatus = InvoiceStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid invoice status: " + status);
            }
        }

        InvoiceFilterDTO filter = InvoiceFilterDTO.builder()
                .status(invoiceStatus)
                .contractId((contractId != null && !contractId.isBlank()) ? UUID.fromString(contractId) : null)
                .searchTerm(searchTerm)
                .build();

        Page<InvoiceSummaryDTO> response = invoiceService.getAllInvoices(
                token,
                pageable,
                filter
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{invoiceId}")
    public ResponseEntity<InvoiceDetailDTO> getInvoiceById(@PathVariable String invoiceId) {
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
