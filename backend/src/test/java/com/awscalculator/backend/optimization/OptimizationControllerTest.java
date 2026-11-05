package com.awscalculator.backend.optimization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.awscalculator.backend.credit.CreditService;
import com.awscalculator.backend.credit.CreditService.CreditReservation;
import com.awscalculator.backend.optimization.dto.OptimizationResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class OptimizationControllerTest {

    private static final Long USER_ID = 7L;
    private static final Long PROJECT_ID = 1L;

    @Mock
    private OptimizationService optimizationService;

    @Mock
    private CreditService creditService;

    @InjectMocks
    private OptimizationController controller;

    private final CreditReservation reservation = new CreditReservation(USER_ID, 1, "AI 비용 최적화 추천", true);

    @Test
    void keepsTheCreditWhenTheAnswerIsAiGenerated() {
        when(creditService.reserve(USER_ID, 1, "AI 비용 최적화 추천")).thenReturn(reservation);
        when(optimizationService.optimize(PROJECT_ID, USER_ID, "ko")).thenReturn(response(true));

        OptimizationResponse result = controller.optimize(PROJECT_ID, USER_ID, "ko");

        assertThat(result.aiGenerated()).isTrue();
        verify(creditService, never()).refund(any());
    }

    @Test
    void refundsWhenTheAnswerIsTheRuleBasedFallback() {
        when(creditService.reserve(USER_ID, 1, "AI 비용 최적화 추천")).thenReturn(reservation);
        when(optimizationService.optimize(PROJECT_ID, USER_ID, "ko")).thenReturn(response(false));

        OptimizationResponse result = controller.optimize(PROJECT_ID, USER_ID, "ko");

        assertThat(result.aiGenerated()).isFalse();
        verify(creditService).refund(reservation);
    }

    @Test
    void refundsAndRethrowsWhenTheCallFails() {
        IllegalArgumentException failure = new IllegalArgumentException("Project has no resources to optimize: 1");
        when(creditService.reserve(USER_ID, 1, "AI 비용 최적화 추천")).thenReturn(reservation);
        when(optimizationService.optimize(PROJECT_ID, USER_ID, "ko")).thenThrow(failure);

        assertThatThrownBy(() -> controller.optimize(PROJECT_ID, USER_ID, "ko")).isSameAs(failure);
        verify(creditService).refund(reservation);
    }

    @Test
    void neverCallsTheAiWhenCreditsAreInsufficient() {
        when(creditService.reserve(USER_ID, 1, "AI 비용 최적화 추천"))
                .thenThrow(new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "크레딧이 부족합니다."));

        assertThatThrownBy(() -> controller.optimize(PROJECT_ID, USER_ID, "ko"))
                .isInstanceOf(ResponseStatusException.class);
        verify(optimizationService, never()).optimize(anyLong(), anyLong(), anyString());
        verify(creditService, never()).refund(any());
    }

    private OptimizationResponse response(boolean aiGenerated) {
        return new OptimizationResponse(
                BigDecimal.TEN, BigDecimal.ONE, BigDecimal.valueOf(9), List.of(),
                aiGenerated ? "claude-api" : "rule-based", aiGenerated);
    }
}
