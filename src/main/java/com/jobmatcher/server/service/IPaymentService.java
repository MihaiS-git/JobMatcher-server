package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.Payment;
import com.jobmatcher.server.model.PaymentDetailDTO;
import com.jobmatcher.server.model.PaymentFilterDTO;
import com.jobmatcher.server.model.PaymentRequestDTO;
import com.jobmatcher.server.model.PaymentSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface IPaymentService {
    Page<PaymentSummaryDTO> getAllPayments(String token, Pageable pageable, PaymentFilterDTO filter);
    PaymentDetailDTO getPaymentById(UUID paymentId);
    PaymentDetailDTO getPaymentByInvoiceId(UUID invoiceId);
    Payment createPayment(PaymentRequestDTO request);
//    void deletePayment(UUID paymentId);
    void markInvoicePaid(UUID invoiceId);
}

