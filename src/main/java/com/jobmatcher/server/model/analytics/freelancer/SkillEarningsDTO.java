package com.jobmatcher.server.model.analytics.freelancer;

import java.math.BigDecimal;

public record SkillEarningsDTO(
        String skillName,
        BigDecimal earnings
) {
}
