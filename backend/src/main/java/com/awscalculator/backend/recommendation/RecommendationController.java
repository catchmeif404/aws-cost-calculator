package com.awscalculator.backend.recommendation;

import com.awscalculator.backend.credit.CreditService;
import com.awscalculator.backend.credit.CreditService.CreditReservation;
import com.awscalculator.backend.recommendation.dto.RecommendationResponse;
import com.awscalculator.backend.recommendation.dto.ServiceRecommendationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Reserve-then-refund (see CreditService): the credit is taken before the LLM call and given back
// if the call throws or the answer is the rule-based fallback.
@RestController
@RequestMapping("/api/architecture-recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final CreditService creditService;

    @PostMapping
    public RecommendationResponse recommend(
            @Valid @RequestBody ServiceRecommendationRequest request,
            @AuthenticationPrincipal Long userId,
            @RequestHeader(value = "X-Locale", required = false, defaultValue = "ko") String locale
    ) {
        CreditReservation reservation = creditService.reserve(userId, 1, "AI 구성 추천");
        RecommendationResponse response;
        try {
            response = recommendationService.recommend(request, locale);
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
