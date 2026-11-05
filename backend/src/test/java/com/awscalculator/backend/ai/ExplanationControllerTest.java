package com.awscalculator.backend.ai;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.awscalculator.backend.ai.dto.ExplanationResponse;
import com.awscalculator.backend.credit.CreditService;
import com.awscalculator.backend.credit.CreditService.CreditReservation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExplanationControllerTest {

    private static final Long USER_ID = 7L;
    private static final Long PROJECT_ID = 1L;

    @Mock
    private ExplanationService explanationService;

    @Mock
    private CreditService creditService;

    @InjectMocks
    private ExplanationController controller;

    private final CreditReservation reservation = new CreditReservation(USER_ID, 1, "AI 설명", true);

    @Test
    void keepsTheCreditWhenTheAnswerIsAiGenerated() {
        when(creditService.reserve(USER_ID, 1, "AI 설명")).thenReturn(reservation);
        when(explanationService.explain(PROJECT_ID, USER_ID, "ko"))
                .thenReturn(new ExplanationResponse("text", "claude-api", true));

        controller.explain(PROJECT_ID, USER_ID, "ko");

        verify(creditService, never()).refund(any());
    }

    @Test
    void refundsWhenTheAnswerIsTheRuleBasedFallback() {
        when(creditService.reserve(USER_ID, 1, "AI 설명")).thenReturn(reservation);
        when(explanationService.explain(PROJECT_ID, USER_ID, "ko"))
                .thenReturn(new ExplanationResponse("text", "rule-based", false));

        controller.explain(PROJECT_ID, USER_ID, "ko");

        verify(creditService).refund(reservation);
    }

    @Test
    void refundsAndRethrowsWhenTheCallFails() {
        IllegalStateException failure = new IllegalStateException("boom");
        when(creditService.reserve(USER_ID, 1, "AI 설명")).thenReturn(reservation);
        when(explanationService.explain(PROJECT_ID, USER_ID, "ko")).thenThrow(failure);

        assertThatThrownBy(() -> controller.explain(PROJECT_ID, USER_ID, "ko")).isSameAs(failure);
        verify(creditService).refund(reservation);
    }
}
