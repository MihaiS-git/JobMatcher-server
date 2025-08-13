package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.PortfolioItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;
import java.util.UUID;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, UUID> {

    @EntityGraph(attributePaths = {"category", "subcategories", "freelancerProfile", "imageUrls"})
    Set<PortfolioItem> findByFreelancerProfileId(UUID freelancerProfileId);

}
