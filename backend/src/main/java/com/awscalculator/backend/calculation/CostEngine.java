package com.awscalculator.backend.calculation;

import com.awscalculator.backend.pricing.ConfigValues;
import com.awscalculator.backend.pricing.PricingCatalog;
import com.awscalculator.backend.pricing.PricingDimensionIndex;
import com.awscalculator.backend.pricing.PricingService;
import com.awscalculator.backend.pricing.PricingSnapshot;
import com.awscalculator.backend.project.Project;
import com.awscalculator.backend.resource.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Deterministic cost calculation. Given a resource's type + configuration, returns its
 * estimated monthly USD cost. This is the only place price math happens — an LLM must never
 * be asked to compute a dollar figure, only to explain one already computed here.
 *
 * Pricing is region-aware: each {@link Resource} belongs to a {@link Project} which carries a
 * region code, resolved to a {@link PricingSnapshot} via {@link PricingCatalog}.
 */
@Component
@RequiredArgsConstructor
public class CostEngine {

    private final PricingService pricingService;
    private final ResourcePricingRuleRepository resourcePricingRuleRepository;

    public BigDecimal monthlyCost(Resource resource) {
        return monthlyCost(resource, resolveRegion(resource));
    }

    public BigDecimal monthlyCost(Resource resource, String region) {
        PricingDimensionIndex pricing = pricingService.dimensionsForRegion(region);
        Map<String, Object> config = resource.getConfiguration();
        List<ResourcePricingRule> rules = resourcePricingRuleRepository.findAllByResourceTypeOrderByRuleOrderAsc(resource.getType());
        if (rules.isEmpty()) {
            throw new IllegalStateException("Missing pricing rules for resource type: " + resource.getType());
        }

        BigDecimal total = rules
                .stream()
                .filter(rule -> applies(rule, config))
                .map(rule -> componentCost(rule, config, pricing))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /** The pricing snapshot that would be used for this resource's own region. */
    public PricingSnapshot pricingFor(Resource resource) {
        return pricingService.forRegion(resolveRegion(resource));
    }

    public BigDecimal usdToKrw(String region) {
        return pricingService.dimensionsForRegion(region).usdToKrw();
    }

    public String resolveRegion(Resource resource) {
        Project project = resource.getProject();
        return (project != null && project.getRegion() != null)
                ? project.getRegion()
                : PricingCatalog.DEFAULT_REGION;
    }

    private boolean applies(ResourcePricingRule rule, Map<String, Object> config) {
        if (isPresent(rule.getDisabledWhenField())) {
            String actual = ConfigValues.text(config, rule.getDisabledWhenField(), "");
            if (rule.getDisabledWhenValue().equalsIgnoreCase(actual)) {
                return false;
            }
        }
        if (!isPresent(rule.getConditionField())) {
            return true;
        }
        String actual = ConfigValues.text(config, rule.getConditionField(), "");
        return rule.getConditionValue().equalsIgnoreCase(actual);
    }

    private BigDecimal componentCost(ResourcePricingRule rule, Map<String, Object> config, PricingDimensionIndex pricing) {
        return switch (rule.getFormulaType()) {
            case PRICE_TIMES_FACTORS -> price(rule, config, pricing)
                    .multiply(quantity(config, rule.getQuantityField()))
                    .multiply(quantity(config, rule.getQuantityField2()))
                    .multiply(quantity(config, rule.getQuantityField3()))
                    .multiply(rule.getMultiplier())
                    .multiply(rule.isMultiplyHours() ? pricing.hoursPerMonth() : BigDecimal.ONE);
        };
    }

    private BigDecimal price(ResourcePricingRule rule, Map<String, Object> config, PricingDimensionIndex pricing) {
        if (!isPresent(rule.getOptionField())) {
            return pricing.scalar(rule.getPriceKey());
        }
        String optionKey = ConfigValues.text(config, rule.getOptionField(), "");
        return pricing.option(rule.getPriceKey(), optionKey);
    }

    private BigDecimal quantity(Map<String, Object> config, String field) {
        if (!isPresent(field)) {
            return BigDecimal.ONE;
        }
        return ConfigValues.number(config, field, BigDecimal.ONE);
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
