package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MilestoneRepository extends JpaRepository<Milestone, UUID> {

}
