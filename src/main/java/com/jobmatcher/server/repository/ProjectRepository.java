package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.Project;
import com.jobmatcher.server.domain.ProjectStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @Query("""
                SELECT p.id
                FROM Project p
                LEFT JOIN p.subcategories s
                WHERE (:profileId IS NULL OR p.customer.id = :profileId OR p.freelancer.id = :profileId)
                  AND (:status IS NULL OR p.status = :status)
                  AND (:categoryId IS NULL OR p.category.id = :categoryId)
                  AND (:subcategoryId IS NULL OR s.id = :subcategoryId OR s.id IS NULL)
                  AND (:searchTerm IS NULL OR :searchTerm = ''
                      OR LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                      OR LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
                GROUP BY p.id
            """)
    List<UUID> findFilteredProjectIds(
            UUID profileId,
            ProjectStatus status,
            Long categoryId,
            Long subcategoryId,
            String searchTerm
    );

    @EntityGraph(attributePaths = {"customer.user", "freelancer.user", "category", "subcategories"})
    List<Project> findByIdIn(List<UUID> ids);

    @Query("""
                SELECT p.id
                FROM Project p
                LEFT JOIN p.subcategories s
                WHERE (:statuses IS NULL OR p.status IN :statuses)
                  AND (:categoryId IS NULL OR p.category.id = :categoryId)
                  AND (:subcategoryId IS NULL OR s.id = :subcategoryId)
                  AND (:searchTerm IS NULL OR :searchTerm = ''
                      OR LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                      OR LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
                GROUP BY p.id
            """)
    List<UUID> findFilteredJobFeedProjectIds(
            List<ProjectStatus> statuses,
            Long categoryId,
            Long subcategoryId,
            String searchTerm
    );


}
