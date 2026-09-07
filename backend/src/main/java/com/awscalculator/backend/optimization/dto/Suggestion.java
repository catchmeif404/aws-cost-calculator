package com.awscalculator.backend.optimization.dto;

import java.math.BigDecimal;

/** estimatedMonthlySavingsUsd is null for purely informational tips with no computed number. */
public record Suggestion(
        String message,
        BigDecimal estimatedMonthlySavingsUsd
) {
}
