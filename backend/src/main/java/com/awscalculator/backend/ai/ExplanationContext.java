package com.awscalculator.backend.ai;

import com.awscalculator.backend.calculation.dto.ResourceCostItem;
import com.awscalculator.backend.optimization.dto.Suggestion;
import java.math.BigDecimal;
import java.util.List;

/**
 * Numbers already computed by {@link com.awscalculator.backend.calculation.CostEngine} and the
 * free rule-based {@link com.awscalculator.backend.optimization.RuleBasedCostOptimizationAdvisor}
 * (not the paid, AI-driven {@link com.awscalculator.backend.optimization.OptimizationService} —
 * this explanation stays free). Every {@link AiExplainer} implementation may only phrase these
 * numbers in natural language — never recompute or invent one.
 */
public record ExplanationContext(
        int monthlyUsers,
        int requestsPerUser,
        String busyTrafficLevel,
        String serviceStage,
        BigDecimal totalMonthlyCostUsd,
        List<ResourceCostItem> resources,
        BigDecimal estimatedSavingsUsd,
        List<Suggestion> suggestions,
        String locale
) {
}
