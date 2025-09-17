package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.Proposal;
import com.jobmatcher.server.domain.ProposalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProposalRepository extends JpaRepository<Proposal, UUID> {

    Page<Proposal> findByProjectId(UUID projectId, Pageable pageable);
    Page<Proposal> findByFreelancerId(UUID freelancerId, Pageable pageable);
    Page<Proposal> findByProjectIdAndStatus(UUID projectId, Pageable pageable, ProposalStatus status);
    Page<Proposal> findByFreelancerIdAndStatus(UUID freelancerId, Pageable pageable, ProposalStatus status);


    Optional<Proposal> findByFreelancerIdAndProjectId(UUID freelancerId, UUID projectId);

}
