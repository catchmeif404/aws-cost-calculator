package com.awscalculator.backend.calculation.dto;

import com.awscalculator.backend.resource.ResourceType;
import java.math.BigDecimal;

public record ResourceCostItem(
        Long resourceId,
        ResourceType type,
        BigDecimal monthlyCost
) {
}
