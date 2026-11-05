package com.awscalculator.backend.recommendation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.awscalculator.backend.credit.CreditService;
import com.awscalculator.backend.credit.CreditService.CreditReservation;
import com.awscalculator.backend.recommendation.dto.RecommendationResponse;
import com.awscalculator.backend.recommendation.dto.ServiceRecommendationRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationControllerTest {

    private static final Long USER_ID = 7L;

    @Mock
    private RecommendationService recommendationService;

    @Mock
    private CreditService creditService;

    @InjectMocks
    private RecommendationController controller;

    private final CreditReservation reservation = new CreditReservation(USER_ID, 1, "AI 구성 추천", true);
    private final ServiceRecommendationRequest request = new ServiceRecommendationRequest(
            "My Service", 10_000, 100, "similar", "mvp", "web_api", "설명", "ap-northeast-2"
    );

    @Test
    void keepsTheCreditWhenTheAnswerIsAiGenerated() {
        when(creditService.reserve(USER_ID, 1, "AI 구성 추천")).thenReturn(reservation);
        when(recommendationService.recommend(request, "ko")).thenReturn(response(true));

        controller.recommend(request, USER_ID, "ko");

        verify(creditService, never()).refund(any());
    }

    @Test
    void refundsWhenTheAnswerIsTheRuleBasedFallback() {
        when(creditService.reserve(USER_ID, 1, "AI 구성 추천")).thenReturn(reservation);
        when(recommendationService.recommend(request, "ko")).thenReturn(response(false));

        controller.recommend(request, USER_ID, "ko");

        verify(creditService).refund(reservation);
    }

    @Test
    void refundsAndRethrowsWhenTheCallFails() {
        IllegalArgumentException failure = new IllegalArgumentException("Unsupported region: mars-1");
        when(creditService.reserve(USER_ID, 1, "AI 구성 추천")).thenReturn(reservation);
        when(recommendationService.recommend(request, "ko")).thenThrow(failure);

        assertThatThrownBy(() -> controller.recommend(request, USER_ID, "ko")).isSameAs(failure);
        verify(creditService).refund(reservation);
    }

    private RecommendationResponse response(boolean aiGenerated) {
        return new RecommendationResponse(
                "tier", "description", 1_000_000L, "mode", "reason",
                aiGenerated ? "claude-api" : "rule-based", aiGenerated,
                BigDecimal.TEN, BigDecimal.valueOf(13_000), List.of(), List.of(), "note");
    }
}
