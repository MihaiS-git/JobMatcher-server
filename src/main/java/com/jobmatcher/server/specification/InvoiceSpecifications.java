package com.jobmatcher.server.specification;

import com.jobmatcher.server.domain.Invoice;
import com.jobmatcher.server.domain.Role;
import com.jobmatcher.server.model.InvoiceFilterDTO;
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

            var contractJoin = root.join("contract");

            if(role == Role.STAFF){
             predicates.add(cb.equal(contractJoin.get("freelancer").get("id"), profileId));
            } else if(role == Role.CUSTOMER){
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
                Path<String> usernamePath;
                if (role == Role.STAFF) {
                    usernamePath = contractJoin.join("freelancer").get("username");
                } else {
                    usernamePath = contractJoin.join("customer").get("username");
                }
                predicates.add(cb.like(cb.lower(usernamePath), likePattern));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
