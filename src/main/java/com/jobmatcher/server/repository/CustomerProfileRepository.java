package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.CustomerProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, UUID> {
    @EntityGraph(attributePaths = {
            "languages",
            "socialMedia"
    })
    Optional<CustomerProfile> findByUserId(UUID userId);
}
