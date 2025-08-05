package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.FreelancerProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FreelancerProfileRepository extends JpaRepository<FreelancerProfile, UUID> {

//    @Query("SELECT fp FROM FreelancerProfile fp " +
//            "LEFT JOIN FETCH fp.jobSubcategories " +
//            "LEFT JOIN FETCH fp.skills " +
//            "LEFT JOIN FETCH fp.languages " +
//            "WHERE fp.user.id = :userId")
//    Optional<FreelancerProfile> findByUserId(@Param("userId") UUID userId);

    @EntityGraph(attributePaths = {
            "jobSubcategories",
            "skills",
            "languages",
            "socialMedia"
    })
    Optional<FreelancerProfile> findByUserId(UUID userId);

}
