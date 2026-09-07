package com.awscalculator.backend.recommendation.dto;

import java.util.Map;

/**
 * One resource an {@link com.awscalculator.backend.recommendation.ArchitectureRecommendationAdvisor}
 * proposed, before it has been checked against the resource catalog. {@code type} is a raw string
 * (not {@link com.awscalculator.backend.resource.ResourceType}) because an LLM can return a typo or
 * a type outside the catalog — {@code ArchitectureRecommendationSanitizer} is what turns this into a
 * trustworthy, catalog-validated resource.
 */
public record AiResourceSpec(String type, Map<String, Object> configuration) {
}
