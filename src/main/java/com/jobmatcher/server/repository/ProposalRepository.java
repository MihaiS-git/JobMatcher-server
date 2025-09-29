package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.Proposal;
import com.jobmatcher.server.domain.ProposalStatus;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ProposalRepository extends JpaRepository<Proposal, UUID> {

    Page<Proposal> findByProjectId(UUID projectId, Pageable pageable);
    Page<Proposal> findByFreelancerId(UUID freelancerId, Pageable pageable);
    Page<Proposal> findByProjectIdAndStatus(UUID projectId, Pageable pageable, ProposalStatus status);
    Page<Proposal> findByFreelancerIdAndStatus(UUID freelancerId, Pageable pageable, ProposalStatus status);


    Optional<Proposal> findByFreelancerIdAndProjectId(UUID freelancerId, UUID projectId);

    boolean existsByFreelancerIdAndProjectId(@NotNull UUID freelancerId, @NotNull UUID projectId);

    @Modifying
    @Query("UPDATE Proposal p SET p.status = :status WHERE p.project.id = :projectId AND p.id <> :acceptedId AND p.status = 'PENDING'")
    void rejectOtherPendingProposals(UUID projectId, UUID acceptedId, ProposalStatus status);

}
