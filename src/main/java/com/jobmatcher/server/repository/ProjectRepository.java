package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;

import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID>, JpaSpecificationExecutor<Project> {

    @EntityGraph(attributePaths = {
            "customer.user",
            "freelancer.user",
            "category",
            "subcategories"
    })
    @Override
    @NonNull
    Page<Project> findAll(Specification<Project> spec, @NonNull Pageable pageable);

}
