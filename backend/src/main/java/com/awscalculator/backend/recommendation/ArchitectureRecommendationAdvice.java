package com.awscalculator.backend.recommendation;

import com.awscalculator.backend.recommendation.dto.AiResourceSpec;
import java.util.List;

public record ArchitectureRecommendationAdvice(
        String architectureName,
        String architectureDescription,
        List<AiResourceSpec> resources,
        String recommendationReason,
        List<String> additionalRecommendations,
        String provider,
        boolean aiGenerated
) {
}
