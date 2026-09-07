package com.awscalculator.backend.resource.dto;

import com.awscalculator.backend.resource.ResourceType;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record AddResourceRequest(
        @NotNull ResourceType type,
        @NotNull Map<String, Object> configuration
) {
}
