package com.awscalculator.backend.project.dto;

import com.awscalculator.backend.pricing.PricingCatalog;
import jakarta.validation.constraints.NotBlank;

public record CreateProjectRequest(
        @NotBlank String name,
        Integer monthlyUsers,
        Integer requestsPerUser,
        String busyTrafficLevel,
        String serviceStage,
        String region
) {
    public String regionOrDefault() {
        String resolved = (region == null || region.isBlank()) ? PricingCatalog.DEFAULT_REGION : region;
        if (!PricingCatalog.SUPPORTED_REGIONS.contains(resolved)) {
            throw new IllegalArgumentException(
                    "Unsupported region: " + resolved + ". Supported: " + PricingCatalog.SUPPORTED_REGIONS);
        }
        return resolved;
    }

    public int monthlyUsersOrDefault() {
        return monthlyUsers == null ? 10_000 : Math.max(0, monthlyUsers);
    }

    public int requestsPerUserOrDefault() {
        return requestsPerUser == null ? 100 : Math.max(0, requestsPerUser);
    }

    public String busyTrafficLevelOrDefault() {
        return (busyTrafficLevel == null || busyTrafficLevel.isBlank()) ? "similar" : busyTrafficLevel;
    }

    public String serviceStageOrDefault() {
        return (serviceStage == null || serviceStage.isBlank()) ? "mvp" : serviceStage;
    }
}
