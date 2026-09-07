package com.awscalculator.backend.calculation.dto;

import java.util.Map;

// Both fields are optional — only present when calculating from the drag builder / an applied AI
// recommendation, respectively. See CalculationResponse.diagramSnapshot/recommendationMetadata.
public record CalculateRequest(Map<String, Object> diagramSnapshot, Map<String, Object> recommendationMetadata) {
}
