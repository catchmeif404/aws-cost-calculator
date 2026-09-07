package com.awscalculator.backend.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import com.awscalculator.backend.recommendation.ArchitectureTier.TierResourceSpec;
import com.awscalculator.backend.recommendation.dto.AiResourceSpec;
import com.awscalculator.backend.resource.ResourceType;
import com.awscalculator.backend.resource.catalog.ResourceCatalogField;
import com.awscalculator.backend.resource.catalog.ResourceCatalogItem;
import com.awscalculator.backend.resource.catalog.ResourceCatalogOption;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ArchitectureRecommendationSanitizerTest {

    private final ResourceCatalogItem rds = new ResourceCatalogItem(
            ResourceType.RDS, "rds", "RDS", "RDS", "Database", "관계형 DB", "tone",
            Map.of("engine", "postgres", "instanceType", "db.t4g.micro", "storageGb", 20),
            List.of(
                    new ResourceCatalogField("engine", "Engine", "select", "postgres",
                            List.of(new ResourceCatalogOption("PostgreSQL", "postgres"), new ResourceCatalogOption("MySQL", "mysql")),
                            null, null, null, false),
                    new ResourceCatalogField("storageGb", "Storage (GB)", "number", 20, List.of(), 20, 1000, 1, true)
            )
    );

    private final List<ResourceCatalogItem> catalog = List.of(rds);

    @Test
    void dropsResourcesWithUnknownType() {
        List<TierResourceSpec> result = ArchitectureRecommendationSanitizer.sanitize(
                List.of(new AiResourceSpec("NOT_A_REAL_TYPE", Map.of())), catalog);

        assertThat(result).isEmpty();
    }

    @Test
    void keepsValidTypeAndFillsMissingFieldsFromDefaults() {
        List<TierResourceSpec> result = ArchitectureRecommendationSanitizer.sanitize(
                List.of(new AiResourceSpec("RDS", Map.of("engine", "mysql"))), catalog);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().type()).isEqualTo(ResourceType.RDS);
        assertThat(result.getFirst().configuration())
                .containsEntry("engine", "mysql")
                .containsEntry("storageGb", 20);
    }

    @Test
    void clampsNumberFieldToCatalogMax() {
        List<TierResourceSpec> result = ArchitectureRecommendationSanitizer.sanitize(
                List.of(new AiResourceSpec("RDS", Map.of("storageGb", 999_999))), catalog);

        assertThat(result.getFirst().configuration()).containsEntry("storageGb", 1000L);
    }

    @Test
    void rejectsSelectValueOutsideCatalogOptionsAndFallsBackToDefault() {
        List<TierResourceSpec> result = ArchitectureRecommendationSanitizer.sanitize(
                List.of(new AiResourceSpec("RDS", Map.of("engine", "drop table users;--"))), catalog);

        assertThat(result.getFirst().configuration()).containsEntry("engine", "postgres");
    }

    @Test
    void dropsConfigurationKeysNotDefinedForType() {
        List<TierResourceSpec> result = ArchitectureRecommendationSanitizer.sanitize(
                List.of(new AiResourceSpec("RDS", Map.of("adminPassword", "hunter2"))), catalog);

        assertThat(result.getFirst().configuration()).doesNotContainKey("adminPassword");
    }
}
