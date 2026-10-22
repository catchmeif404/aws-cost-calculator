package com.awscalculator.backend.ai;

import com.awscalculator.backend.ai.dto.ExplanationResponse;
import com.awscalculator.backend.credit.CreditService;
import com.awscalculator.backend.credit.CreditService.CreditReservation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Spends a credit — see ExplanationService's javadoc for why this used to be free and no longer
// is. Same reserve-then-refund shape as Optimization/RecommendationController (see CreditService):
// the rule-based fallback explanation is free.
@RestController
@RequestMapping("/api/projects/{projectId}/explain")
@RequiredArgsConstructor
public class ExplanationController {

    private final ExplanationService explanationService;
    private final CreditService creditService;

    @PostMapping
    public ExplanationResponse explain(
            @PathVariable Long projectId,
            @AuthenticationPrincipal Long userId,
            @RequestHeader(value = "X-Locale", required = false, defaultValue = "ko") String locale
    ) {
        CreditReservation reservation = creditService.reserve(userId, 1, "AI 설명");
        ExplanationResponse response;
        try {
            response = explanationService.explain(projectId, userId, locale);
        } catch (RuntimeException e) {
            creditService.refund(reservation);
            throw e;
        }
        if (!response.aiGenerated()) {
            creditService.refund(reservation);
        }
        return response;
    }
}
