package com.awscalculator.backend.metrics.dto;

import java.util.List;

public record ViewsSummary(long total, List<DailyCount> daily) {
}
