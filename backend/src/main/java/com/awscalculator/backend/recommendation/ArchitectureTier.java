package com.awscalculator.backend.recommendation;

import com.awscalculator.backend.resource.ResourceType;
import java.util.List;
import java.util.Map;

public record ArchitectureTier(
        String name,
        String description,
        List<TierResourceSpec> resources
) {
    public record TierResourceSpec(ResourceType type, Map<String, Object> configuration) {
    }
}
