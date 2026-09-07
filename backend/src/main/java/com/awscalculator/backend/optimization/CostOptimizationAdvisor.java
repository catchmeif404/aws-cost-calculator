package com.awscalculator.backend.optimization;

public interface CostOptimizationAdvisor {

    String name();

    /**
     * Freely proposes cost-optimization suggestions for the given project's current architecture.
     * An AI-backed advisor may estimate its own savings figures (there's no CostEngine call it can
     * defer to for "what would this cost if reconfigured this other way" in general); a rule-based
     * advisor re-prices concrete alternatives through CostEngine instead. Either way,
     * {@link OptimizationService} is what turns the resulting suggestions into the response's
     * current/optimized/estimatedSavings totals — an advisor only supplies {@link
     * com.awscalculator.backend.optimization.dto.Suggestion} entries.
     */
    CostOptimizationAdvice advise(CostOptimizationContext context);
}
