package com.awscalculator.backend.calculation.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CalculationResponse(
        BigDecimal totalMonthlyCost,
        BigDecimal totalMonthlyCostKrw,
        String currency,
        List<ResourceCostItem> resources,
        // The drag builder's system diagram (rects/groups/connections), passed through opaquely —
        // this backend never interprets its shape, only stores and echoes back whatever the
        // frontend sent at calculate time (or null, e.g. when calculating from the wizard flow).
        Map<String, Object> diagramSnapshot,
        // Same opaque pass-through, for the AI recommendation (tier name, description, reason,
        // additional recommendations) these resources came from — null unless this calculation
        // was reached by applying one.
        Map<String, Object> recommendationMetadata
) {
}
