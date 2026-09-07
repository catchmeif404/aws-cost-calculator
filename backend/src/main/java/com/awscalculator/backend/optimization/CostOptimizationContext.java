package com.awscalculator.backend.optimization;

import com.awscalculator.backend.project.Project;
import com.awscalculator.backend.resource.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Everything a {@link CostOptimizationAdvisor} needs to reason about one project's current
 * architecture: the raw entities (so the rule-based advisor can synthesize comparison resources,
 * e.g. an ARM64 variant, and re-price them via CostEngine) plus each resource's already-computed
 * monthly cost (so no advisor needs to recompute pricing itself).
 */
public record CostOptimizationContext(
        Project project,
        List<Resource> resources,
        Map<Long, BigDecimal> monthlyCostByResourceId,
        BigDecimal totalMonthlyCostUsd,
        String locale
) {
}
