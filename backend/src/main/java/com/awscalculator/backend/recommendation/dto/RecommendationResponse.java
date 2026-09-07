package com.awscalculator.backend.recommendation.dto;

import java.math.BigDecimal;
import java.util.List;

public record RecommendationResponse(
        String tierName,
        String description,
        long estimatedMonthlyRequests,
        String analysisMode,
        String recommendationReason,
        String provider,
        boolean aiGenerated,
        BigDecimal totalMonthlyCostUsd,
        BigDecimal totalMonthlyCostKrw,
        List<TierResourceCost> resources,
        List<String> additionalRecommendations,
        String note
) {
}
