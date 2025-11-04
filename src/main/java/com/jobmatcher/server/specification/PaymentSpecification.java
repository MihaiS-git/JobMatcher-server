package com.jobmatcher.server.specification;

import com.jobmatcher.server.domain.Payment;
import com.jobmatcher.server.domain.Role;
import com.jobmatcher.server.model.PaymentFilterDTO;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public class PaymentSpecification {
    public static Specification<Payment> withFiltersAndRole(
            PaymentFilterDTO filter,
            Role role,
            UUID profileId
    ) {
        log.info("Building Payment specification with filters: {}, role: {}, profileId: {}",
                filter, role, profileId);

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            log.info("Joining related entities for filtering");
            var invoiceJoin = root.join("invoice", JoinType.LEFT);
            var contractJoin = invoiceJoin.join("contract", JoinType.LEFT);

            if (role == Role.STAFF) {
                predicates.add(cb.equal(contractJoin.get("freelancer").get("id"), profileId));
            } else if (role == Role.CUSTOMER) {
                predicates.add(cb.equal(contractJoin.get("customer").get("id"), profileId));
            }

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getSearchTerm() != null && !filter.getSearchTerm().isBlank()) {
                String term = filter.getSearchTerm().trim();

                // UUID search
                try {
                    UUID uuid = UUID.fromString(term);
                    predicates.add(cb.equal(invoiceJoin.get("id"), uuid));
                } catch (IllegalArgumentException ignored) {
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
