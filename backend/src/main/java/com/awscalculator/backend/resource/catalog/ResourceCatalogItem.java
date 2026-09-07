package com.awscalculator.backend.resource.catalog;

import com.awscalculator.backend.resource.ResourceType;
import java.util.List;
import java.util.Map;

public record ResourceCatalogItem(
        ResourceType type,
        String kind,
        String title,
        String shortName,
        String category,
        String description,
        String tone,
        Map<String, Object> defaults,
        List<ResourceCatalogField> fields
) {
}
