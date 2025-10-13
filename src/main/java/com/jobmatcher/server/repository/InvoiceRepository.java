package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.contract WHERE i.id = :invoiceId")
    Optional<Invoice> findByIdWithContract(@Param("invoiceId") UUID invoiceId);
}
