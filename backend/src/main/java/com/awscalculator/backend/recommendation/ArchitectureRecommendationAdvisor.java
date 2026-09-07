package com.awscalculator.backend.recommendation;

import com.awscalculator.backend.recommendation.dto.ServiceRecommendationRequest;
import com.awscalculator.backend.resource.catalog.ResourceCatalogItem;
import java.util.List;

public interface ArchitectureRecommendationAdvisor {

    String name();

    /**
     * Freely composes an architecture (which resource types, how many, with what configuration)
     * for the given service profile. {@code catalog} is the full set of resource types/fields/
     * options this backend knows how to price — an advisor may only propose resources shaped by
     * it, but is not required to validate that itself; {@link RecommendationService} always runs
     * the result through {@link ArchitectureRecommendationSanitizer} before pricing it.
     */
    ArchitectureRecommendationAdvice advise(
            ServiceRecommendationRequest request,
            long estimatedMonthlyRequests,
            List<ResourceCatalogItem> catalog,
            String locale
    );
}
