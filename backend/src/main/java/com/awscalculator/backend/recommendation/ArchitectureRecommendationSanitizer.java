package com.awscalculator.backend.recommendation;

import com.awscalculator.backend.recommendation.ArchitectureTier.TierResourceSpec;
import com.awscalculator.backend.recommendation.dto.AiResourceSpec;
import com.awscalculator.backend.resource.ResourceType;
import com.awscalculator.backend.resource.catalog.ResourceCatalogField;
import com.awscalculator.backend.resource.catalog.ResourceCatalogItem;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Turns an advisor's freely-composed {@link AiResourceSpec} list into resources this app can
 * actually price: only {@link ResourceType}s that exist in the resource catalog survive, and each
 * one's configuration is rebuilt field-by-field from the catalog's own defaults/options/min/max —
 * an advisor (especially an LLM) can suggest a type or field value that doesn't exist, but it can
 * never make a resource escape the catalog's shape.
 */
final class ArchitectureRecommendationSanitizer {

    private ArchitectureRecommendationSanitizer() {
    }

    static List<TierResourceSpec> sanitize(List<AiResourceSpec> proposed, List<ResourceCatalogItem> catalog) {
        if (proposed == null || proposed.isEmpty()) {
            return List.of();
        }

        Map<String, ResourceCatalogItem> catalogByTypeName = catalog.stream()
                .collect(Collectors.toMap(item -> item.type().name(), item -> item, (a, b) -> a));

        List<TierResourceSpec> sanitized = new ArrayList<>();
        for (AiResourceSpec spec : proposed) {
            if (spec == null || spec.type() == null || spec.type().isBlank()) {
                continue;
            }
            ResourceCatalogItem item = catalogByTypeName.get(spec.type().trim().toUpperCase(Locale.ROOT));
            if (item == null) {
                continue;
            }
            sanitized.add(new TierResourceSpec(item.type(), sanitizeConfiguration(spec.configuration(), item)));
        }
        return sanitized;
    }

    private static Map<String, Object> sanitizeConfiguration(Map<String, Object> proposed, ResourceCatalogItem item) {
        Map<String, Object> source = proposed == null ? Map.of() : proposed;
        Map<String, Object> config = new LinkedHashMap<>(item.defaults());

        for (ResourceCatalogField field : item.fields()) {
            if (!source.containsKey(field.key())) {
                continue;
            }
            Object value = source.get(field.key());
            switch (field.type()) {
                case "number" -> sanitizeNumber(field, value).ifPresent(number -> config.put(field.key(), number));
                case "select" -> {
                    if (matchesOption(field, value)) {
                        config.put(field.key(), value);
                    }
                }
                default -> config.put(field.key(), value);
            }
        }
        return config;
    }

    private static java.util.Optional<Object> sanitizeNumber(ResourceCatalogField field, Object value) {
        BigDecimal number;
        try {
            number = value instanceof Number n ? new BigDecimal(n.toString()) : new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return java.util.Optional.empty();
        }
        if (field.min() != null && number.compareTo(BigDecimal.valueOf(field.min())) < 0) {
            number = BigDecimal.valueOf(field.min());
        }
        if (field.max() != null && number.compareTo(BigDecimal.valueOf(field.max())) > 0) {
            number = BigDecimal.valueOf(field.max());
        }
        boolean isIntegral = number.stripTrailingZeros().scale() <= 0;
        return java.util.Optional.of(isIntegral ? (Object) number.longValue() : (Object) number.doubleValue());
    }

    private static boolean matchesOption(ResourceCatalogField field, Object value) {
        if (field.options().isEmpty()) {
            return true;
        }
        String candidate = String.valueOf(value);
        return field.options().stream().anyMatch(option -> String.valueOf(option.value()).equals(candidate));
    }
}
