package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.PortfolioItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, UUID> {

    @EntityGraph(attributePaths = {"category", "subcategories", "freelancerProfile", "imageUrls"})
    Set<PortfolioItem> findByFreelancerProfileId(UUID freelancerProfileId);

    @Query("SELECT pi FROM PortfolioItem pi LEFT JOIN FETCH pi.imageUrls WHERE pi.id = :id")
    Optional<PortfolioItem> findByIdWithImages(@Param("id") UUID portfolioItemId);

}
