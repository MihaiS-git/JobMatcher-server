package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.FreelancerProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FreelancerProfileRepository extends JpaRepository<FreelancerProfile, UUID> {


    @EntityGraph(attributePaths = {
            "jobSubcategories",
            "skills",
            "languages",
            "socialMedia"
    })
    Optional<FreelancerProfile> findByUserId(UUID userId);

    @EntityGraph(attributePaths = {
            "user",
            "jobSubcategories",
            "skills",
            "languages",
            "socialMedia"
    })
    Optional<FreelancerProfile> findById(UUID userId);

}
