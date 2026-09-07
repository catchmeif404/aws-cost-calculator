package com.awscalculator.backend.calculation.dto;

import java.math.BigDecimal;
import java.time.Instant;

// One past calculation, as it was priced at save time — not recomputed from current pricing.
// calculationId (not projectId) is the row's stable identity: a project can in principle be
// calculated more than once (nothing prevents calling /calculate again on the same project), so
// projectId alone isn't guaranteed unique across this list.
public record ProjectHistoryResponse(
        Long calculationId,
        Long projectId,
        String projectName,
        String region,
        BigDecimal totalMonthlyCostUsd,
        BigDecimal totalMonthlyCostKrw,
        Instant calculatedAt
) {
}
