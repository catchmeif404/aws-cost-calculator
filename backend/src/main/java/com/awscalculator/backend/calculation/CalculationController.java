package com.awscalculator.backend.calculation;

import com.awscalculator.backend.calculation.dto.CalculateRequest;
import com.awscalculator.backend.calculation.dto.CalculationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// No credit charge here — the deterministic cost calculation itself stays free (per product
// decision: only the AI-driven recommendation costs a credit, since that's the part that's
// actually expensive to run). See RecommendationController for the one place credits are spent.
@RestController
@RequestMapping("/api/projects/{projectId}/calculate")
@RequiredArgsConstructor
public class CalculationController {

    private final CalculationService calculationService;

    @PostMapping
    public CalculationResponse calculate(
            @PathVariable Long projectId,
            @AuthenticationPrincipal Long userId,
            @RequestBody(required = false) CalculateRequest request
    ) {
        var diagramSnapshot = request == null ? null : request.diagramSnapshot();
        var recommendationMetadata = request == null ? null : request.recommendationMetadata();
        return calculationService.calculate(projectId, userId, diagramSnapshot, recommendationMetadata);
    }

    // Re-fetches the last saved calculation for the history report (마이페이지 계산 기록) — priced as
    // of when it was saved, not recomputed against current pricing. Requires the caller to own
    // the project (see CalculationService.getSavedCalculation).
    @GetMapping
    public CalculationResponse getSaved(@PathVariable Long projectId, @AuthenticationPrincipal Long userId) {
        return calculationService.getSavedCalculation(projectId, userId);
    }
}
