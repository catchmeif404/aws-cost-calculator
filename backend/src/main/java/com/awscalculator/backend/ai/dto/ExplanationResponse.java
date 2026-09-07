package com.awscalculator.backend.ai.dto;

public record ExplanationResponse(
        String explanation,
        String provider,
        boolean aiGenerated
) {
}
