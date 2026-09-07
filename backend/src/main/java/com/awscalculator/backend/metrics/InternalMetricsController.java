package com.awscalculator.backend.metrics;

import com.awscalculator.backend.metrics.dto.MetricsSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gated by {@link InternalApiKeyFilter}, not Spring Security's role system - this path falls
 * under SecurityConfig's blanket {@code /api/**} permitAll, and the real access control happens
 * earlier in the filter chain.
 */
@RestController
@RequestMapping("/api/internal/metrics")
@RequiredArgsConstructor
public class InternalMetricsController {

    private final InternalMetricsService internalMetricsService;

    @GetMapping("/summary")
    public MetricsSummaryResponse summary() {
        return internalMetricsService.getSummary();
    }
}
