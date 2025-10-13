package com.jobmatcher.server.specification;

import com.jobmatcher.server.domain.Payment;
import com.jobmatcher.server.domain.Role;
import com.jobmatcher.server.model.PaymentFilterDTO;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PaymentSpecification {
    public static Specification<Payment> withFiltersAndRole(
            PaymentFilterDTO filter,
            Role role,
            UUID profileId
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            var invoiceJoin = root.join("invoice");
            var contractJoin = invoiceJoin.join("contract");

            if (role == Role.STAFF) {
                predicates.add(cb.equal(contractJoin.get("freelancer").get("id"), profileId));
            } else if (role == Role.CUSTOMER) {
                predicates.add(cb.equal(contractJoin.get("customer").get("id"), profileId));
            }

            if (filter.getContractId() != null) {
                predicates.add(cb.equal(contractJoin.get("id"), filter.getContractId()));
            }

            if (filter.getInvoiceId() != null) {
                predicates.add(cb.equal(invoiceJoin.get("id"), filter.getInvoiceId()));
            }

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getSearchTerm() != null && !filter.getSearchTerm().isBlank()) {
                String likePattern = "%" + filter.getSearchTerm().toLowerCase() + "%";
                Predicate usernamePredicate;
                if (role == Role.STAFF) {
                    usernamePredicate = cb.like(cb.lower(contractJoin.join("customer").get("username")), likePattern);
                } else {
                    usernamePredicate = cb.like(cb.lower(contractJoin.join("freelancer").get("username")), likePattern);
                }
                predicates.add(usernamePredicate);
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
