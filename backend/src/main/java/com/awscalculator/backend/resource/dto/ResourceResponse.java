package com.awscalculator.backend.resource.dto;

import com.awscalculator.backend.resource.Resource;
import com.awscalculator.backend.resource.ResourceType;
import java.util.Map;

public record ResourceResponse(
        Long id,
        ResourceType type,
        Map<String, Object> configuration
) {
    public static ResourceResponse from(Resource resource) {
        return new ResourceResponse(resource.getId(), resource.getType(), resource.getConfiguration());
    }
}
