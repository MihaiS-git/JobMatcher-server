package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @Query("SELECT p FROM Payment p JOIN p.contract c WHERE c.customer.id = :customerId")
    Page<Payment> findAllByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);

    @Query("SELECT p FROM Payment p JOIN p.contract c WHERE c.freelancer.id = :freelancerId")
    Page<Payment> findAllByFreelancerId(@Param("freelancerId") UUID freelancerId, Pageable pageable);

    Optional<Payment> findByInvoiceId(UUID invoiceId);
}
