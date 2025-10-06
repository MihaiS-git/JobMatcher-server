package com.jobmatcher.server.service;

import com.jobmatcher.server.model.PaymentDetailDTO;
import com.jobmatcher.server.model.PaymentFilterDTO;
import com.jobmatcher.server.model.PaymentRequestDTO;
import com.jobmatcher.server.model.PaymentSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface IPaymentService {
    Page<PaymentSummaryDTO> getAllPaymentsByProfileId(String authHeader, String profileId, PaymentFilterDTO filter, Pageable pageable);
    PaymentDetailDTO getPaymentByInvoiceId(UUID invoiceId);
    PaymentDetailDTO createPayment(PaymentRequestDTO request);
    PaymentDetailDTO updatePayment(UUID paymentId, PaymentRequestDTO request);
    void deletePayment(UUID paymentId);
}
