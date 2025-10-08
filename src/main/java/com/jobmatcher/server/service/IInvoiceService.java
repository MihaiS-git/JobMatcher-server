package com.jobmatcher.server.service;

import com.jobmatcher.server.model.InvoiceDetailDTO;
import com.jobmatcher.server.model.InvoiceFilterDTO;
import com.jobmatcher.server.model.InvoiceRequestDTO;
import com.jobmatcher.server.model.InvoiceSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IInvoiceService {
    InvoiceDetailDTO getInvoiceById(UUID invoiceId);
    InvoiceDetailDTO createInvoice(InvoiceRequestDTO request);
    InvoiceDetailDTO updateInvoice(UUID invoiceId, InvoiceRequestDTO request);
    void deleteInvoice(UUID invoiceId);

    Page<InvoiceSummaryDTO> getAllInvoices(
            String token,
            Pageable pageable,
            InvoiceFilterDTO filter
    );
}
