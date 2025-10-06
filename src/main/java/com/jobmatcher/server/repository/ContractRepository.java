package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ContractRepository extends JpaRepository<Contract, UUID>,
        JpaSpecificationExecutor<Contract> {

    @Query("SELECT c FROM Contract c WHERE c.project.id = :projectId")
    Optional<Contract> findByProjectId(@Param("projectId") UUID projectId);

}
