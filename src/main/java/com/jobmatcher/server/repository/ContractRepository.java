package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.Contract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ContractRepository extends JpaRepository<Contract, UUID> {

    @Query("SELECT c FROM Contract c WHERE c.customer.id = :customerId")
    Page<Contract> findAllByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.freelancer.id = :freelancerId")
    Page<Contract> findAllByFreelancerId(@Param("freelancerId") UUID freelancerId, Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.project.id = :projectId")
    Optional<Contract> findByProjectId(@Param("projectId") UUID projectId);

}
