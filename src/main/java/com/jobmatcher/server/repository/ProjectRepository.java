package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.Project;
import com.jobmatcher.server.domain.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID>, JpaSpecificationExecutor<Project> {

    @Query("""
                SELECT DISTINCT p.id FROM Project p
                LEFT JOIN p.subcategories s
                WHERE (:profileId IS NULL OR p.customer.id = :profileId OR p.freelancer.id = :profileId)
                  AND (:status IS NULL OR p.status = :status)
                  AND (:categoryId IS NULL OR p.category.id = :categoryId)
                  AND (:subcategoryId IS NULL OR s.id = :subcategoryId)
                  AND (:searchTerm IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                       OR LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
            """)
    Page<UUID> findFilteredProjectIds(
            UUID profileId,
            ProjectStatus status,
            Long categoryId,
            Long subcategoryId,
            String searchTerm,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"customer.user", "freelancer.user", "category", "subcategories"})
    List<Project> findByIdIn(List<UUID> ids);

}
