package com.jobmatcher.server.specification;

import com.jobmatcher.server.domain.Invoice;
import com.jobmatcher.server.domain.Role;
import com.jobmatcher.server.model.InvoiceFilterDTO;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InvoiceSpecifications {
    public static Specification<Invoice> withFiltersAndRole(
            InvoiceFilterDTO filter,
            Role role,
            UUID profileId
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Explicit join for lazy association
            var contractJoin = root.join("contract", JoinType.LEFT);

            if (role == Role.STAFF) {
                predicates.add(cb.equal(contractJoin.get("freelancer").get("id"), profileId));
            } else if (role == Role.CUSTOMER) {
                predicates.add(cb.equal(contractJoin.get("customer").get("id"), profileId));
            }

            if (filter.getContractId() != null) {
                predicates.add(cb.equal(root.get("contract").get("id"), filter.getContractId()));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getSearchTerm() != null && !filter.getSearchTerm().isBlank()) {
                String likePattern = "%" + filter.getSearchTerm().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(contractJoin.get("title")), likePattern),
                        cb.like(cb.lower(contractJoin.get("description")), likePattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
