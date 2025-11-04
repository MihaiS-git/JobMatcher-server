package com.jobmatcher.server.specification;

import com.jobmatcher.server.domain.Project;
import com.jobmatcher.server.domain.Role;
import com.jobmatcher.server.model.ProjectFilterDTO;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProjectSpecification {

    public static Specification<Project> withFiltersAndRole(ProjectFilterDTO filter, Role role, UUID profileId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Explicit joins for lazy associations
            var customerJoin = root.join("customer", JoinType.LEFT);
            var freelancerJoin = root.join("freelancer", JoinType.LEFT);
            var subcategoriesJoin = root.joinSet("subcategories", JoinType.LEFT);

            // Role based filtering
            if(role == Role.STAFF){
                predicates.add(cb.equal(root.get("freelancer").get("id"), profileId));
            } else if(role == Role.CUSTOMER){
                predicates.add(cb.equal(root.get("customer").get("id"), profileId));
            }

            //Status filter
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

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

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
