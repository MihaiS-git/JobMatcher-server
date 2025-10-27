package com.jobmatcher.server.model.analytics.freelancer;

import java.math.BigDecimal;

public record MonthlyEarningsDTO(
        Integer year,
        Integer month,
        BigDecimal total
) {
}
