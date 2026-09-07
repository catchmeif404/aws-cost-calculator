package com.awscalculator.backend.metrics.dto;

import java.time.LocalDate;

public record DailyCount(LocalDate date, long count) {
}
