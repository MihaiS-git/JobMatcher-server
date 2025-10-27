package com.jobmatcher.server.model.analytics.freelancer;

import java.math.BigDecimal;

public record TopClientDTO(
        String clientName,
        BigDecimal totalSpent
) {
}
