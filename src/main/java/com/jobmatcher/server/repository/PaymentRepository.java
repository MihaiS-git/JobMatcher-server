package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findByInvoiceId(UUID invoiceId);

    boolean existsByInvoiceId(UUID id);
}
