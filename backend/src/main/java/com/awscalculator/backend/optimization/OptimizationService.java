package com.awscalculator.backend.optimization;

import com.awscalculator.backend.calculation.CostEngine;
import com.awscalculator.backend.optimization.dto.OptimizationResponse;
import com.awscalculator.backend.optimization.dto.Suggestion;
import com.awscalculator.backend.project.Project;
import com.awscalculator.backend.project.ProjectService;
import com.awscalculator.backend.resource.Resource;
import com.awscalculator.backend.resource.ResourceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the (now AI-driven) cost optimization feature: tries whichever {@link
 * CostOptimizationAdvisor} bean's name() matches ai.optimization.provider (codex-cli locally,
 * claude-api/gemini-api once deployed — see application.properties), and falls back to {@link
 * RuleBasedCostOptimizationAdvisor} if it fails, isn't configured, or returns no suggestions.
 * Same provider-select/fallback shape as {@link com.awscalculator.backend.recommendation.RecommendationService}
 * and {@link com.awscalculator.backend.ai.ExplanationService}. Unlike those, this is charged —
 * see OptimizationController — since every call here is a real LLM call, not a rephrasing of an
 * already-computed number.
 */
@Service
@RequiredArgsConstructor
public class OptimizationService {

    private static final Logger log = LoggerFactory.getLogger(OptimizationService.class);

    private final ProjectService projectService;
    private final ResourceRepository resourceRepository;
    private final CostEngine costEngine;
    private final List<CostOptimizationAdvisor> advisors;
    private final RuleBasedCostOptimizationAdvisor fallbackAdvisor;

    @Value("${ai.optimization.provider:codex-cli}")
    private String advisorProvider;

    @Transactional(readOnly = true)
    public OptimizationResponse optimize(Long projectId, Long userId, String locale) {
        Project project = projectService.getOwnedOrThrow(projectId, userId);
        List<Resource> resources = resourceRepository.findByProjectId(projectId);
        if (resources.isEmpty()) {
            throw new IllegalArgumentException("Project has no resources to optimize: " + projectId);
        }

        Map<Long, BigDecimal> costs = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Resource resource : resources) {
            BigDecimal cost = costEngine.monthlyCost(resource);
            costs.put(resource.getId(), cost);
            total = total.add(cost);
        }
        total = total.setScale(2, RoundingMode.HALF_UP);

        CostOptimizationContext context = new CostOptimizationContext(project, resources, costs, total, locale);
        CostOptimizationAdvice advice = adviceFor(context);

        BigDecimal savings = advice.suggestions().stream()
                .map(Suggestion::estimatedMonthlySavingsUsd)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        if (savings.compareTo(total) > 0) {
            savings = total;
        }
        BigDecimal optimizedTotal = total.subtract(savings).setScale(2, RoundingMode.HALF_UP);

        return new OptimizationResponse(total, optimizedTotal, savings, advice.suggestions(), advice.provider(), advice.aiGenerated());
    }

    private CostOptimizationAdvice adviceFor(CostOptimizationContext context) {
        CostOptimizationAdvisor primary = advisors.stream()
                .filter(advisor -> advisor.name().equals(advisorProvider))
                .findFirst()
                .orElse(null);

        if (primary != null) {
            try {
                CostOptimizationAdvice advice = primary.advise(context);
                if (!advice.suggestions().isEmpty()) {
                    return advice;
                }
                log.warn("Primary cost optimization advisor ({}) returned no suggestions, falling back to rule-based",
                        primary.name());
            } catch (RuntimeException e) {
                log.warn("Primary cost optimization advisor ({}) failed, falling back to rule-based: {}",
                        primary.name(), e.getMessage());
            }
        } else {
            log.warn("ai.optimization.provider '{}' matches no advisor bean; using rule-based fallback", advisorProvider);
        }

        return fallbackAdvisor.advise(context);
    }
}
