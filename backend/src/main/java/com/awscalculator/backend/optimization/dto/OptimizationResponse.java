package com.awscalculator.backend.optimization.dto;

import java.math.BigDecimal;
import java.util.List;

public record OptimizationResponse(
        BigDecimal currentMonthlyCost,
        BigDecimal optimizedMonthlyCost,
        BigDecimal estimatedSavings,
        List<Suggestion> suggestions,
        String provider,
        boolean aiGenerated
) {
}
