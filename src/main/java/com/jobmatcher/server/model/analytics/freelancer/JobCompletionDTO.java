package com.jobmatcher.server.model.analytics.freelancer;

import java.math.BigDecimal;

public record JobCompletionDTO(
        Long completed,
        Long total,
        BigDecimal rate
) {
}
