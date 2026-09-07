package com.awscalculator.backend.recommendation;

import com.awscalculator.backend.recommendation.dto.ServiceRecommendationRequest;
import com.awscalculator.backend.resource.catalog.ResourceCatalogField;
import com.awscalculator.backend.resource.catalog.ResourceCatalogItem;
import com.awscalculator.backend.resource.catalog.ResourceCatalogOption;
import java.util.List;

final class ArchitectureRecommendationPromptBuilder {

    private ArchitectureRecommendationPromptBuilder() {
    }

    static String build(
            ServiceRecommendationRequest request,
            long estimatedMonthlyRequests,
            List<ResourceCatalogItem> catalog,
            String locale
    ) {
        boolean english = "en".equals(locale);
        String languageInstruction = english
                ? "Write architectureName, architectureDescription, recommendationReason and "
                        + "additionalRecommendations in English."
                : "In Korean output, never use the word \"피크\"; say \"가장 바쁜 시간대\" instead.";
        String jsonShape = english
                ? """
                JSON shape:
                {
                  "architectureName": "Short English name for this architecture, e.g. \\"Event-driven Commerce API\\"",
                  "architectureDescription": "English, 1 sentence describing the shape of this architecture.",
                  "resources": [
                    {"type": "ECS", "configuration": {"cpu": 0.5, "memory": 1, "tasks": 2, "architecture": "arm64"}},
                    {"type": "RDS", "configuration": {"engine": "postgres", "instanceType": "db.t4g.micro", "storageGb": 20}}
                  ],
                  "recommendationReason": "English explanation, 2-3 short sentences on why this combination fits.",
                  "additionalRecommendations": ["English bullet", "English bullet"]
                }
                """
                : """
                JSON shape:
                {
                  "architectureName": "Korean short name for this architecture, e.g. \\"이벤트 기반 커머스 API\\"",
                  "architectureDescription": "Korean 1 sentence describing the shape of this architecture.",
                  "resources": [
                    {"type": "ECS", "configuration": {"cpu": 0.5, "memory": 1, "tasks": 2, "architecture": "arm64"}},
                    {"type": "RDS", "configuration": {"engine": "postgres", "instanceType": "db.t4g.micro", "storageGb": 20}}
                  ],
                  "recommendationReason": "Korean explanation, 2-3 short sentences on why this combination fits.",
                  "additionalRecommendations": ["Korean bullet", "Korean bullet"]
                }
                """;

        return """
                You are an AWS solution architect for an early-stage cost calculator.
                Return only valid JSON. Do not include markdown fences.
                %s

                Freely design the AWS architecture for the service profile below by choosing which
                resources to include, how many, and how to configure each one. You are NOT limited to
                a fixed template — pick any combination of the catalog resource types that fits the
                service, and omit anything unnecessary. Do not calculate or invent dollar costs
                yourself; the backend prices whatever resources you choose with a deterministic
                pricing engine.

                Only use resource types from this catalog, and for each resource only set
                configuration keys listed for that type (any key/value outside this catalog will be
                discarded, and a select field is restricted to the option values shown):

                %s

                %s

                Service profile:
                - serviceName: %s
                - serviceType: %s
                - monthlyUsers: %d
                - requestsPerUser: %d
                - estimatedMonthlyRequests: %d
                - busyTrafficLevel: %s
                - serviceStage: %s
                - region: %s
                - serviceDescription: %s
                """.formatted(
                languageInstruction,
                catalogText(catalog),
                jsonShape,
                request.serviceName(),
                request.serviceTypeOrDefault(),
                request.monthlyUsersOrDefault(),
                request.requestsPerUserOrDefault(),
                estimatedMonthlyRequests,
                request.busyTrafficLevelOrDefault(),
                request.serviceStageOrDefault(),
                request.region(),
                request.serviceDescriptionOrDefault()
        );
    }

    private static String catalogText(List<ResourceCatalogItem> catalog) {
        StringBuilder sb = new StringBuilder();
        for (ResourceCatalogItem item : catalog) {
            sb.append("- ").append(item.type().name())
                    .append(" (").append(item.category()).append(", ").append(item.description()).append("): ");
            sb.append(fieldsText(item));
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String fieldsText(ResourceCatalogItem item) {
        List<ResourceCatalogField> fields = item.fields();
        if (fields.isEmpty()) {
            return "(no configuration fields)";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(fieldText(fields.get(i)));
        }
        return sb.toString();
    }

    private static String fieldText(ResourceCatalogField field) {
        if ("select".equals(field.type())) {
            String options = field.options().stream()
                    .map(ResourceCatalogOption::value)
                    .map(String::valueOf)
                    .reduce((a, b) -> a + "|" + b)
                    .orElse("");
            return field.key() + "[select:" + options + ", default=" + field.defaultValue() + "]";
        }
        String range = (field.min() != null || field.max() != null)
                ? (field.min() == null ? "" : field.min()) + "-" + (field.max() == null ? "" : field.max())
                : "any";
        return field.key() + "[number:" + range + ", default=" + field.defaultValue() + "]";
    }
}
