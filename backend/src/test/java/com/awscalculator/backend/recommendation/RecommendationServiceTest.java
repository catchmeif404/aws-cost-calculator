package com.awscalculator.backend.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.awscalculator.backend.ai.AiExplanationException;
import com.awscalculator.backend.calculation.CostEngine;
import com.awscalculator.backend.recommendation.dto.AiResourceSpec;
import com.awscalculator.backend.recommendation.dto.RecommendationResponse;
import com.awscalculator.backend.recommendation.dto.ServiceRecommendationRequest;
import com.awscalculator.backend.resource.Resource;
import com.awscalculator.backend.resource.ResourceType;
import com.awscalculator.backend.resource.catalog.ResourceCatalogField;
import com.awscalculator.backend.resource.catalog.ResourceCatalogItem;
import com.awscalculator.backend.resource.catalog.ResourceCatalogService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private RuleBasedArchitectureRecommendationAdvisor fallbackAdvisor;

    @Mock
    private ResourceCatalogService resourceCatalogService;

    @Mock
    private CostEngine costEngine;

    private final ResourceCatalogItem s3 = new ResourceCatalogItem(
            ResourceType.S3, "s3", "S3", "S3", "Storage", "파일 저장소", "tone",
            Map.of("storageGb", 10),
            List.of(new ResourceCatalogField("storageGb", "Storage (GB)", "number", 10, List.of(), 1, null, 1, true))
    );

    private final List<ResourceCatalogItem> catalog = List.of(s3);
    private final ServiceRecommendationRequest request = new ServiceRecommendationRequest(
            "My Service", 10_000, 100, "similar", "mvp", "web_api", "설명", "ap-northeast-2"
    );

    @BeforeEach
    void setUp() {
        when(resourceCatalogService.items()).thenReturn(catalog);
        when(costEngine.monthlyCost(any(Resource.class), anyString())).thenReturn(BigDecimal.TEN);
        when(costEngine.usdToKrw(anyString())).thenReturn(BigDecimal.valueOf(1300));
    }

    @Test
    void usesPrimaryAdvisorResourcesWhenTheyValidateAgainstCatalog() {
        ArchitectureRecommendationAdvisor primary = fakeAdvisor("claude-api", new ArchitectureRecommendationAdvice(
                "AI 구성", "설명", List.of(new AiResourceSpec("S3", Map.of("storageGb", 50))),
                "이유", List.of(), "claude-api", true
        ));
        RecommendationService service = newService(primary);

        RecommendationResponse response = service.recommend(request, "ko");

        assertThat(response.aiGenerated()).isTrue();
        assertThat(response.provider()).isEqualTo("claude-api");
        assertThat(response.resources()).hasSize(1);
        assertThat(response.resources().getFirst().type()).isEqualTo(ResourceType.S3);
        assertThat(response.resources().getFirst().configuration()).containsEntry("storageGb", 50L);
    }

    @Test
    void fallsBackToRuleBasedWhenPrimaryAdvisorThrows() {
        ArchitectureRecommendationAdvisor primary = fakeAdvisorThatThrows("claude-api");
        stubFallback();
        RecommendationService service = newService(primary);

        RecommendationResponse response = service.recommend(request, "ko");

        assertThat(response.aiGenerated()).isFalse();
        assertThat(response.provider()).isEqualTo("rule-based");
        assertThat(response.resources()).hasSize(1);
    }

    @Test
    void fallsBackToRuleBasedWhenPrimaryAdvisorResourcesAllFailCatalogValidation() {
        ArchitectureRecommendationAdvisor primary = fakeAdvisor("claude-api", new ArchitectureRecommendationAdvice(
                "AI 구성", "설명", List.of(new AiResourceSpec("NOT_A_REAL_TYPE", Map.of())),
                "이유", List.of(), "claude-api", true
        ));
        stubFallback();
        RecommendationService service = newService(primary);

        RecommendationResponse response = service.recommend(request, "ko");

        assertThat(response.aiGenerated()).isFalse();
        assertThat(response.provider()).isEqualTo("rule-based");
    }

    private void stubFallback() {
        when(fallbackAdvisor.advise(any(), org.mockito.ArgumentMatchers.anyLong(), any(), anyString())).thenReturn(
                new ArchitectureRecommendationAdvice(
                        "미니멀", "설명", List.of(new AiResourceSpec("S3", Map.of("storageGb", 5))),
                        "이유", List.of(), "rule-based", false
                )
        );
    }

    private RecommendationService newService(ArchitectureRecommendationAdvisor primary) {
        RecommendationService service = new RecommendationService(
                List.of(primary, fallbackAdvisor), fallbackAdvisor, resourceCatalogService, costEngine
        );
        ReflectionTestUtils.setField(service, "advisorProvider", "claude-api");
        return service;
    }

    private ArchitectureRecommendationAdvisor fakeAdvisor(String name, ArchitectureRecommendationAdvice advice) {
        return new ArchitectureRecommendationAdvisor() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public ArchitectureRecommendationAdvice advise(
                    ServiceRecommendationRequest request, long estimatedMonthlyRequests,
                    List<ResourceCatalogItem> catalog, String locale
            ) {
                return advice;
            }
        };
    }

    private ArchitectureRecommendationAdvisor fakeAdvisorThatThrows(String name) {
        return new ArchitectureRecommendationAdvisor() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public ArchitectureRecommendationAdvice advise(
                    ServiceRecommendationRequest request, long estimatedMonthlyRequests,
                    List<ResourceCatalogItem> catalog, String locale
            ) {
                throw new AiExplanationException("boom");
            }
        };
    }
}
