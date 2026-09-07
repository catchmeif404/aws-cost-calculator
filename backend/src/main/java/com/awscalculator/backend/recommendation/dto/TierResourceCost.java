package com.awscalculator.backend.recommendation.dto;

import com.awscalculator.backend.resource.ResourceType;
import java.math.BigDecimal;
import java.util.Map;

public record TierResourceCost(
        ResourceType type,
        Map<String, Object> configuration,
        BigDecimal monthlyCost
) {
}
