package com.jobmatcher.server.model.analytics.customer;

import java.math.BigDecimal;

public record TopFreelancerDTO(
        String freelancerName,
        BigDecimal totalEarned
) {
}
