package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProposalRepository extends JpaRepository<Proposal, UUID> {
}
