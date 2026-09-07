package com.awscalculator.backend.resource.catalog;

import java.util.List;

public record ResourceCatalogField(
        String key,
        String label,
        String type,
        Object defaultValue,
        List<ResourceCatalogOption> options,
        Integer min,
        Integer max,
        Integer step,
        boolean costRelevant
) {
}
