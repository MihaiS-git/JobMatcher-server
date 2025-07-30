package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.FreelancerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FreelancerProfileRepository extends JpaRepository<FreelancerProfile, UUID> {
}
