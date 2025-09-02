package com.jobmatcher.server.specification;

import com.jobmatcher.server.domain.Project;
import com.jobmatcher.server.domain.ProjectStatus;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class ProjectSpecifications {
    public static Specification<Project> filterProjects(
            UUID profileId,
            ProjectStatus status,
            Long categoryId,
            Long subcategoryId,
            String searchTerm
    ) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();
            if (profileId != null) {
                predicates = cb.and(predicates, cb.or(
                        cb.equal(root.get("customer").get("id"), profileId),
                        cb.equal(root.get("freelancer").get("id"), profileId)
                ));
            }
            if (status != null) {
                predicates = cb.and(predicates, root.get("status").in(status));
            }
            if (categoryId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("category").get("id"), categoryId));
            }
            if (subcategoryId != null) {
                var subjoin = root.joinSet("subcategories", JoinType.LEFT);
                predicates = cb.and(predicates, subjoin.get("id").in(subcategoryId));
                assert query != null;
                query.distinct(true);
            }
            if (searchTerm != null && !searchTerm.isBlank()) {
                String likePattern = "%" + searchTerm.toLowerCase() + "%";
                predicates = cb.and(predicates, cb.or(
                                cb.like(cb.lower(root.get("title")), likePattern),
                                cb.like(cb.lower(root.get("description")), likePattern)
                        )
                );
            }
            return predicates;
        };
    }
}
