package com.awscalculator.backend.optimization;

import com.awscalculator.backend.calculation.CostEngine;
import com.awscalculator.backend.optimization.dto.Suggestion;
import com.awscalculator.backend.pricing.ConfigValues;
import com.awscalculator.backend.pricing.PricingSnapshot;
import com.awscalculator.backend.resource.Resource;
import com.awscalculator.backend.resource.ResourceType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Deterministic fallback used when no primary (LLM-backed) {@link CostOptimizationAdvisor} is
 * configured, or when the primary one fails or returns nothing usable — same role as {@link
 * com.awscalculator.backend.recommendation.RuleBasedArchitectureRecommendationAdvisor} plays for
 * architecture recommendations. Also used directly (bypassing the AI-provider selection and
 * credit charge) by {@link com.awscalculator.backend.ai.ExplanationService} to fold a couple of
 * quick, free savings tips into the AI cost explanation.
 * <p>
 * Every dollar figure here is computed from {@link CostEngine} / {@link PricingSnapshot} — this
 * class never invents a number.
 */
@Component
@RequiredArgsConstructor
public class RuleBasedCostOptimizationAdvisor implements CostOptimizationAdvisor {

    private static final String SMALLEST_RDS_INSTANCE = "db.t4g.micro";
    private static final String SMALLEST_REDIS_NODE = "cache.t4g.micro";

    private final CostEngine costEngine;

    @Override
    public String name() {
        return "rule-based";
    }

    @Override
    public CostOptimizationAdvice advise(CostOptimizationContext context) {
        List<Suggestion> suggestions = new ArrayList<>();
        List<Resource> resources = context.resources();

        for (Resource resource : resources) {
            BigDecimal cost = context.monthlyCostByResourceId().get(resource.getId());
            switch (resource.getType()) {
                case ECS -> gravitonSuggestion(resource, cost, suggestions);
                case RDS -> rdsDownsizeSuggestion(resource, cost, suggestions);
                case REDIS -> redisDownsizeSuggestion(resource, cost, suggestions);
                default -> { /* no rule yet */ }
            }
        }

        boolean hasEcsWithoutNatGateway = resources.stream().anyMatch(r -> r.getType() == ResourceType.ECS)
                && resources.stream().noneMatch(r -> r.getType() == ResourceType.NAT_GATEWAY);
        if (hasEcsWithoutNatGateway) {
            suggestions.add(new Suggestion(
                    "ECS 태스크가 프라이빗 서브넷에서 외부 API를 호출한다면 NAT Gateway 비용(약 $32/월 + 데이터 전송)이 별도로 붙을 수 있어요. "
                            + "필요 없다면 NAT Gateway 없이 구성하거나 VPC Endpoint로 대체하는 걸 검토하세요.",
                    null
            ));
        }

        boolean hasDirectDataTransferWithoutCloudFront = resources.stream().anyMatch(r -> r.getType() == ResourceType.DATA_TRANSFER)
                && resources.stream().noneMatch(r -> r.getType() == ResourceType.CLOUDFRONT);
        if (hasDirectDataTransferWithoutCloudFront) {
            suggestions.add(new Suggestion(
                    "정적 콘텐츠 트래픽이 많다면 CloudFront를 앞단에 두고 캐싱하면 오리진(EC2/ECS) 데이터 전송량 자체가 줄어들어요. "
                            + "캐시 적중률이 높을수록 절감 폭이 커집니다.",
                    null
            ));
        }

        return new CostOptimizationAdvice(suggestions, name(), false);
    }

    private void gravitonSuggestion(Resource resource, BigDecimal currentCost, List<Suggestion> suggestions) {
        String architecture = ConfigValues.text(resource.getConfiguration(), "architecture", "x86_64");
        if ("arm64".equalsIgnoreCase(architecture)) {
            return;
        }

        Map<String, Object> armConfig = new HashMap<>(resource.getConfiguration());
        armConfig.put("architecture", "arm64");
        Resource armResource = new Resource(resource.getProject(), resource.getType(), armConfig);
        BigDecimal armCost = costEngine.monthlyCost(armResource);

        BigDecimal savings = currentCost.subtract(armCost).setScale(2, RoundingMode.HALF_UP);
        if (savings.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal percent = savings.divide(currentCost, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP);

        suggestions.add(new Suggestion(
                "ECS Fargate를 ARM64(Graviton)로 전환하면 약 " + percent
                        + "% 저렴해져요. 약 $" + savings + "/월 절감 예상.",
                savings
        ));
    }

    private void rdsDownsizeSuggestion(Resource resource, BigDecimal currentCost, List<Suggestion> suggestions) {
        String instanceType = ConfigValues.text(resource.getConfiguration(), "instanceType", SMALLEST_RDS_INSTANCE);
        if (SMALLEST_RDS_INSTANCE.equals(instanceType)) {
            return;
        }

        PricingSnapshot pricing = costEngine.pricingFor(resource);
        BigDecimal currentHourly = pricing.rdsInstanceHour().get(instanceType);
        BigDecimal smallestHourly = pricing.rdsInstanceHour().get(SMALLEST_RDS_INSTANCE);
        if (currentHourly == null || smallestHourly == null || currentHourly.compareTo(smallestHourly) <= 0) {
            return;
        }

        BigDecimal savings = currentHourly.subtract(smallestHourly)
                .multiply(pricing.hoursPerMonth())
                .setScale(2, RoundingMode.HALF_UP);

        suggestions.add(new Suggestion(
                "RDS 트래픽이 많지 않다면 " + instanceType + " 대신 " + SMALLEST_RDS_INSTANCE
                        + "로 다운사이징을 검토하세요. 약 $" + savings + "/월 절감 예상.",
                savings
        ));
    }

    private void redisDownsizeSuggestion(Resource resource, BigDecimal currentCost, List<Suggestion> suggestions) {
        String nodeType = ConfigValues.text(resource.getConfiguration(), "nodeType", SMALLEST_REDIS_NODE);
        if (SMALLEST_REDIS_NODE.equals(nodeType)) {
            return;
        }

        PricingSnapshot pricing = costEngine.pricingFor(resource);
        BigDecimal currentHourly = pricing.redisNodeHour().get(nodeType);
        BigDecimal smallestHourly = pricing.redisNodeHour().get(SMALLEST_REDIS_NODE);
        if (currentHourly == null || smallestHourly == null || currentHourly.compareTo(smallestHourly) <= 0) {
            return;
        }

        int nodes = ConfigValues.integer(resource.getConfiguration(), "nodes", 1);
        BigDecimal savings = currentHourly.subtract(smallestHourly)
                .multiply(BigDecimal.valueOf(nodes))
                .multiply(pricing.hoursPerMonth())
                .setScale(2, RoundingMode.HALF_UP);

        suggestions.add(new Suggestion(
                "Redis 트래픽이 많지 않다면 " + nodeType + " 대신 " + SMALLEST_REDIS_NODE
                        + "로 다운사이징을 검토하세요. 약 $" + savings + "/월 절감 예상.",
                savings
        ));
    }
}
