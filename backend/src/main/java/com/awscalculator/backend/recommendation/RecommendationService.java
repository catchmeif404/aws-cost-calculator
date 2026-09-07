package com.awscalculator.backend.recommendation;

import com.awscalculator.backend.calculation.CostEngine;
import com.awscalculator.backend.pricing.PricingCatalog;
import com.awscalculator.backend.recommendation.ArchitectureTier.TierResourceSpec;
import com.awscalculator.backend.recommendation.dto.RecommendationResponse;
import com.awscalculator.backend.recommendation.dto.ServiceRecommendationRequest;
import com.awscalculator.backend.recommendation.dto.TierResourceCost;
import com.awscalculator.backend.resource.Resource;
import com.awscalculator.backend.resource.catalog.ResourceCatalogItem;
import com.awscalculator.backend.resource.catalog.ResourceCatalogService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Recommends an AWS architecture for a service profile. The advisor (AI or rule-based) freely
 * chooses which resources to include and how to configure them; {@link CostEngine} is the only
 * place that ever turns those choices into dollars, and
 * {@link ArchitectureRecommendationSanitizer} is what keeps an advisor's choices inside the
 * resource catalog's actual types/fields/options before they ever reach the pricing engine.
 * Which adapter is primary is picked via ai.recommendation.provider (see application.properties).
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final List<ArchitectureRecommendationAdvisor> advisors;
    private final RuleBasedArchitectureRecommendationAdvisor fallbackAdvisor;
    private final ResourceCatalogService resourceCatalogService;
    private final CostEngine costEngine;

    @Value("${ai.recommendation.provider:codex-cli}")
    private String advisorProvider;

    public RecommendationResponse recommend(ServiceRecommendationRequest request, String locale) {
        String region = (request.region() == null || request.region().isBlank())
                ? PricingCatalog.DEFAULT_REGION
                : request.region();
        if (!PricingCatalog.SUPPORTED_REGIONS.contains(region)) {
            throw new IllegalArgumentException(
                    "Unsupported region: " + region + ". Supported: " + PricingCatalog.SUPPORTED_REGIONS);
        }

        int monthlyUsers = request.monthlyUsersOrDefault();
        int requestsPerUser = request.requestsPerUserOrDefault();
        long estimatedMonthlyRequests = (long) monthlyUsers * requestsPerUser;
        List<ResourceCatalogItem> catalog = resourceCatalogService.items();

        Advice advice = adviceFor(request, estimatedMonthlyRequests, catalog, locale);

        List<TierResourceCost> resources = advice.specs().stream()
                .map(spec -> new TierResourceCost(
                        spec.type(),
                        spec.configuration(),
                        costEngine.monthlyCost(toTransientResource(spec), region)
                ))
                .toList();

        BigDecimal totalUsd = resources.stream()
                .map(TierResourceCost::monthlyCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalKrw = totalUsd.multiply(costEngine.usdToKrw(region)).setScale(0, RoundingMode.HALF_UP);

        boolean english = "en".equals(locale);
        return new RecommendationResponse(
                advice.raw().architectureName(),
                advice.raw().architectureDescription(),
                estimatedMonthlyRequests,
                advice.raw().aiGenerated()
                        ? (english ? "AI Recommendation" : "AI 추천")
                        : (english ? "Default Recommendation" : "기본 추천"),
                advice.raw().recommendationReason(),
                advice.raw().provider(),
                advice.raw().aiGenerated(),
                totalUsd,
                totalKrw,
                resources,
                advice.raw().additionalRecommendations(),
                advice.raw().aiGenerated()
                        ? (english
                                ? "Cost is computed by the pricing engine; the configuration was designed by "
                                        + advice.raw().provider() + " based on the service profile."
                                : "비용은 가격 엔진으로 계산하고, 구성은 " + advice.raw().provider() + "가 서비스 특성을 보고 직접 구성했습니다.")
                        : (english
                                ? advisorProvider + " recommendation failed, responded with the default recommendation instead. "
                                        + "Cost is computed by the pricing engine."
                                : advisorProvider + " 추천에 실패해 기본 추천으로 응답했습니다. 비용은 가격 엔진으로 계산했습니다.")
        );
    }

    private Advice adviceFor(
            ServiceRecommendationRequest request,
            long estimatedMonthlyRequests,
            List<ResourceCatalogItem> catalog,
            String locale
    ) {
        ArchitectureRecommendationAdvisor primary = advisors.stream()
                .filter(advisor -> advisor.name().equals(advisorProvider))
                .findFirst()
                .orElse(null);

        if (primary != null) {
            try {
                ArchitectureRecommendationAdvice raw = primary.advise(request, estimatedMonthlyRequests, catalog, locale);
                List<TierResourceSpec> specs = ArchitectureRecommendationSanitizer.sanitize(raw.resources(), catalog);
                if (!specs.isEmpty()) {
                    return new Advice(raw, specs);
                }
                log.warn("Primary architecture recommendation advisor ({}) returned no catalog-valid resources, "
                        + "falling back to rule-based", primary.name());
            } catch (RuntimeException e) {
                log.warn("Primary architecture recommendation advisor ({}) failed, falling back to rule-based: {}",
                        primary.name(), e.getMessage());
            }
        } else {
            log.warn("ai.recommendation.provider '{}' matches no advisor bean; using rule-based fallback", advisorProvider);
        }

        ArchitectureRecommendationAdvice raw = fallbackAdvisor.advise(request, estimatedMonthlyRequests, catalog, locale);
        List<TierResourceSpec> specs = ArchitectureRecommendationSanitizer.sanitize(raw.resources(), catalog);
        return new Advice(raw, specs);
    }

    private Resource toTransientResource(TierResourceSpec spec) {
        return new Resource(null, spec.type(), spec.configuration());
    }

    private record Advice(ArchitectureRecommendationAdvice raw, List<TierResourceSpec> specs) {
    }
}
