package com.jobmatcher.server.model.analytics.customer;

import java.math.BigDecimal;

public record MonthlySpendingDTO(
        Integer year,
        Integer month,
        BigDecimal total
) {
}
