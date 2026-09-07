package com.awscalculator.backend.optimization;

import com.awscalculator.backend.optimization.dto.Suggestion;
import java.util.List;

public record CostOptimizationAdvice(
        List<Suggestion> suggestions,
        String provider,
        boolean aiGenerated
) {
}
