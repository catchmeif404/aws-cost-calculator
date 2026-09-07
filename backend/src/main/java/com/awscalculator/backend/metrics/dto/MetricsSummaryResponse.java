package com.awscalculator.backend.metrics.dto;

// Shape is fixed across every catchmeif404 project's /api/internal/metrics/summary (see
// catchmeif404-admin's docs/DESIGN.md) - a field is null when this project has nothing to report
// for it, not omitted, so the aggregator can use one generic client for every project.
public record MetricsSummaryResponse(Long memberCount, ViewsSummary views, ActiveUsersSummary activeUsers) {
}
