package com.jobmatcher.server.specification;

import com.jobmatcher.server.domain.Project;
import com.jobmatcher.server.domain.ProjectStatus;
import com.jobmatcher.server.model.JobFeedProjectFilterDTO;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class JobFeedProjectSpecification {

    public static Specification<Project> withFilters(JobFeedProjectFilterDTO filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), ProjectStatus.OPEN));

            // Explicit joins for lazy associations
            var customerJoin = root.join("customer", JoinType.LEFT);
            var subcategoriesJoin = root.joinSet("subcategories", JoinType.LEFT);

            // Category filter
            if (filter.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), filter.getCategoryId()));
            }

            // Subcategory filter
            if (filter.getSubcategoryId() != null) {
                predicates.add(cb.equal(subcategoriesJoin.get("id"), filter.getSubcategoryId()));
            }

            // Search term filter
            if (filter.getSearchTerm() != null && !filter.getSearchTerm().isBlank()) {
                String likePattern = "%" + filter.getSearchTerm().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), likePattern),
                        cb.like(cb.lower(root.get("description")), likePattern)
                ));
            }

            assert query != null;
            query.orderBy(cb.desc(root.get("createdAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
