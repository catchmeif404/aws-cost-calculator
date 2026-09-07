package com.awscalculator.backend.calculation;

import com.awscalculator.backend.calculation.dto.CalculationResponse;
import com.awscalculator.backend.calculation.dto.ProjectHistoryResponse;
import com.awscalculator.backend.calculation.dto.ResourceCostItem;
import com.awscalculator.backend.common.NotFoundException;
import com.awscalculator.backend.pricing.PricingCatalog;
import com.awscalculator.backend.project.Project;
import com.awscalculator.backend.project.ProjectService;
import com.awscalculator.backend.resource.Resource;
import com.awscalculator.backend.resource.ResourceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CalculationService {

    private final ProjectService projectService;
    private final ResourceRepository resourceRepository;
    private final CalculationRepository calculationRepository;
    private final CalculationResourceCostRepository calculationResourceCostRepository;
    private final CostEngine costEngine;

    @Transactional
    public CalculationResponse calculate(
            Long projectId,
            Long userId,
            Map<String, Object> diagramSnapshot,
            Map<String, Object> recommendationMetadata
    ) {
        Project project = projectService.getOwnedOrThrow(projectId, userId);
        List<Resource> resources = resourceRepository.findByProjectId(projectId);
        if (resources.isEmpty()) {
            throw new IllegalArgumentException("Project has no resources to calculate: " + projectId);
        }

        List<ResourceCostItem> items = resources.stream()
                .map(r -> new ResourceCostItem(r.getId(), r.getType(), costEngine.monthlyCost(r)))
                .toList();

        BigDecimal total = items.stream()
                .map(ResourceCostItem::monthlyCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalKrw = total.multiply(costEngine.usdToKrw(project.getRegion())).setScale(0, RoundingMode.HALF_UP);

        Calculation calculation = calculationRepository.save(
                new Calculation(project, total, totalKrw, "USD", diagramSnapshot, recommendationMetadata));
        calculationResourceCostRepository.saveAll(
                items.stream()
                        .map(item -> new CalculationResourceCost(calculation, item.resourceId(), item.type(), item.monthlyCost()))
                        .toList()
        );

        return new CalculationResponse(total, totalKrw, "USD", items, diagramSnapshot, recommendationMetadata);
    }

    @Transactional(readOnly = true)
    public List<ProjectHistoryResponse> listForUser(Long userId) {
        return calculationRepository.findByProjectUserIdOrderByCreatedAtDesc(userId).stream()
                .map(calculation -> new ProjectHistoryResponse(
                        calculation.getId(),
                        calculation.getProject().getId(),
                        calculation.getProject().getName(),
                        calculation.getProject().getRegion(),
                        calculation.getTotalMonthlyCost(),
                        calculation.getTotalMonthlyCostKrw(),
                        calculation.getCreatedAt()
                ))
                .toList();
    }

    // Returns the last saved calculation's numbers exactly as they were, not recomputed against
    // current pricing — same "cost as of save time" contract as listForUser. 404s (rather than
    // 403) if the project has no saved calculation or belongs to a different user, so a caller
    // can't distinguish "not yours" from "doesn't exist".
    @Transactional(readOnly = true)
    public CalculationResponse getSavedCalculation(Long projectId, Long userId) {
        Calculation calculation = calculationRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)
                .orElseThrow(() -> new NotFoundException("No saved calculation for project: " + projectId));

        Project owner = calculation.getProject();
        Long ownerId = owner.getUser() == null ? null : owner.getUser().getId();
        if (!Objects.equals(ownerId, userId)) {
            throw new NotFoundException("No saved calculation for project: " + projectId);
        }

        List<ResourceCostItem> items = calculationResourceCostRepository.findByCalculationIdOrderByIdAsc(calculation.getId())
                .stream()
                .map(cost -> new ResourceCostItem(cost.getResourceId(), cost.getType(), cost.getMonthlyCost()))
                .toList();

        return new CalculationResponse(
                calculation.getTotalMonthlyCost(),
                calculation.getTotalMonthlyCostKrw(),
                calculation.getCurrency(),
                items,
                calculation.getDiagramSnapshot(),
                calculation.getRecommendationMetadata()
        );
    }
}
