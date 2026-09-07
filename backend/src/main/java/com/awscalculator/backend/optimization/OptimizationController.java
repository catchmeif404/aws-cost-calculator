package com.awscalculator.backend.optimization;

import com.awscalculator.backend.credit.CreditService;
import com.awscalculator.backend.optimization.dto.OptimizationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// AI-driven cost optimization spends a credit — unlike /calculate, this is a real LLM call each
// time, not a rephrasing of an already-computed number. See RecommendationController for the
// other credit-charging endpoint.
@RestController
@RequestMapping("/api/projects/{projectId}/optimize")
@RequiredArgsConstructor
public class OptimizationController {

    private final OptimizationService optimizationService;
    private final CreditService creditService;

    @PostMapping
    public OptimizationResponse optimize(
            @PathVariable Long projectId,
            @AuthenticationPrincipal Long userId,
            @RequestHeader(value = "X-Locale", required = false, defaultValue = "ko") String locale
    ) {
        creditService.ensureSufficient(userId, 1);
        OptimizationResponse response = optimizationService.optimize(projectId, userId, locale);
        creditService.spendUsage(userId, 1, "AI 비용 최적화 추천");
        return response;
    }
}
