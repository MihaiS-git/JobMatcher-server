package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    @Query("SELECT i FROM Invoice i JOIN i.contract c WHERE c.customer.id = :customerId")
    Page<Invoice> findAllByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);

    @Query("SELECT i FROM Invoice i JOIN i.contract c WHERE c.freelancer.id = :freelancerId")
    Page<Invoice> findAllByFreelancerId(@Param("freelancerId") UUID freelancerId, Pageable pageable);
}
