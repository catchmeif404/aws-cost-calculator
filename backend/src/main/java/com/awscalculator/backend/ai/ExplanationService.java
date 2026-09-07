package com.awscalculator.backend.ai;

import com.awscalculator.backend.ai.dto.ExplanationResponse;
import com.awscalculator.backend.calculation.CostEngine;
import com.awscalculator.backend.calculation.dto.ResourceCostItem;
import com.awscalculator.backend.optimization.CostOptimizationAdvice;
import com.awscalculator.backend.optimization.CostOptimizationContext;
import com.awscalculator.backend.optimization.RuleBasedCostOptimizationAdvisor;
import com.awscalculator.backend.optimization.dto.Suggestion;
import com.awscalculator.backend.project.Project;
import com.awscalculator.backend.project.ProjectService;
import com.awscalculator.backend.resource.Resource;
import com.awscalculator.backend.resource.ResourceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the AI explanation feature: recomputes the (already deterministic) cost +
 * optimization numbers, tries whichever {@link AiExplainer} bean's name() matches
 * ai.explainer.provider (codex-cli locally, claude-api once deployed — see
 * application.properties), and falls back to {@link RuleBasedAiExplainer} if it fails or isn't
 * configured. The caller never sees an error — worst case they get the rule-based explanation
 * instead of the AI one.
 * <p>
 * Optimization tips folded into the explanation come from {@link RuleBasedCostOptimizationAdvisor}
 * directly, not the paid {@link com.awscalculator.backend.optimization.OptimizationService} —
 * that part alone would be free. But once ai.explainer.provider points at a real API (not
 * codex-cli), this method itself makes a real, billed LLM call, so ExplanationController charges
 * 1 credit per call the same way Optimization/RecommendationController do.
 */
@Service
@RequiredArgsConstructor
public class ExplanationService {

    private static final Logger log = LoggerFactory.getLogger(ExplanationService.class);

    private final ProjectService projectService;
    private final ResourceRepository resourceRepository;
    private final RuleBasedCostOptimizationAdvisor optimizationAdvisor;
    private final List<AiExplainer> explainers;
    private final RuleBasedAiExplainer fallbackExplainer;
    private final CostEngine costEngine;

    @Value("${ai.explainer.provider:codex-cli}")
    private String explainerProvider;

    public ExplanationResponse explain(Long projectId, Long userId, String locale) {
        Project project = projectService.getOwnedOrThrow(projectId, userId);
        List<Resource> resources = resourceRepository.findByProjectId(projectId);
        if (resources.isEmpty()) {
            throw new IllegalArgumentException("Project has no resources to explain: " + projectId);
        }

        Map<Long, BigDecimal> costs = new LinkedHashMap<>();
        List<ResourceCostItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Resource resource : resources) {
            BigDecimal cost = costEngine.monthlyCost(resource);
            costs.put(resource.getId(), cost);
            items.add(new ResourceCostItem(resource.getId(), resource.getType(), cost));
            total = total.add(cost);
        }
        total = total.setScale(2, RoundingMode.HALF_UP);

        CostOptimizationAdvice advice = optimizationAdvisor.advise(
                new CostOptimizationContext(project, resources, costs, total, locale));
        BigDecimal savings = advice.suggestions().stream()
                .map(Suggestion::estimatedMonthlySavingsUsd)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        if (savings.compareTo(total) > 0) {
            savings = total;
        }

        ExplanationContext context = new ExplanationContext(
                project.getMonthlyUsers(),
                project.getRequestsPerUser(),
                project.getBusyTrafficLevel(),
                project.getServiceStage(),
                total, items, savings, advice.suggestions(), locale
        );

        AiExplainer primary = explainers.stream()
                .filter(explainer -> explainer.name().equals(explainerProvider))
                .findFirst()
                .orElse(null);

        if (primary != null) {
            try {
                String text = primary.explain(context);
                return new ExplanationResponse(text, primary.name(), true);
            } catch (AiExplanationException e) {
                log.warn("Primary AI explainer ({}) failed, falling back to rule-based: {}",
                        primary.name(), e.getMessage());
            }
        } else {
            log.warn("ai.explainer.provider '{}' matches no AiExplainer bean; using rule-based fallback", explainerProvider);
        }
        String text = fallbackExplainer.explain(context);
        return new ExplanationResponse(text, fallbackExplainer.name(), false);
    }
}
