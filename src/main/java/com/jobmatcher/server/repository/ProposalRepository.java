package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.Proposal;
import com.jobmatcher.server.domain.ProposalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProposalRepository extends JpaRepository<Proposal, UUID> {

    Page<Proposal> findByProjectId(UUID projectId, Pageable pageable);
    Page<Proposal> findByProjectIdAndStatus(UUID projectId, Pageable pageable, ProposalStatus status);

}
