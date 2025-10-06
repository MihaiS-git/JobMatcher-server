package com.jobmatcher.server.specification;

import com.jobmatcher.server.domain.Contract;
import com.jobmatcher.server.domain.Role;
import com.jobmatcher.server.model.ContractFilterDTO;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ContractSpecifications {

    public static Specification<Contract> withFiltersAndRole(ContractFilterDTO filter, Role role, String profileId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if(role == Role.STAFF){
                predicates.add(cb.equal(root.get("freelancer").get("id"), UUID.fromString(profileId)));
            } else if(role == Role.CUSTOMER){
                predicates.add(cb.equal(root.get("customer").get("id"), UUID.fromString(profileId)));
            }

            if (filter.getFreelancerId() != null) {
                predicates.add(cb.equal(root.get("freelancer").get("id"), filter.getFreelancerId()));
            }
            if (filter.getCustomerId() != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), filter.getCustomerId()));
            }
            if (filter.getProjectId() != null) {
                predicates.add(cb.equal(root.get("project").get("id"), filter.getProjectId()));
            }
            if (filter.getProposalId() != null) {
                predicates.add(cb.equal(root.get("proposal").get("id"), filter.getProposalId()));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getPaymentStatus() != null) {
                predicates.add(cb.equal(root.get("paymentStatus"), filter.getPaymentStatus()));
            }
            if (filter.getPaymentType() != null) {
                predicates.add(cb.equal(root.get("paymentType"), filter.getPaymentType()));
            }
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
