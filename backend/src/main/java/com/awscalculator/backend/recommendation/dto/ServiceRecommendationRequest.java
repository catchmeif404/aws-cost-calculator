package com.awscalculator.backend.recommendation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// Every String field here gets embedded directly into the AI prompt
// (ArchitectureRecommendationPromptBuilder) — size limits keep a single 1-credit call from
// blowing up into a huge, expensive LLM request. monthlyUsers/requestsPerUser are similarly
// capped to keep estimatedMonthlyRequests (their product) sane.
public record ServiceRecommendationRequest(
        @NotBlank @Size(max = 200) String serviceName,
        @PositiveOrZero @Max(10_000_000) Integer monthlyUsers,
        @PositiveOrZero @Max(100_000) Integer requestsPerUser,
        @Size(max = 50) String busyTrafficLevel,
        @Size(max = 50) String serviceStage,
        @Size(max = 50) String serviceType,
        @Size(max = 2000) String serviceDescription,
        @Size(max = 50) String region
) {
    public int monthlyUsersOrDefault() {
        return monthlyUsers == null ? 10_000 : monthlyUsers;
    }

    public int requestsPerUserOrDefault() {
        return requestsPerUser == null ? 100 : requestsPerUser;
    }

    public String busyTrafficLevelOrDefault() {
        return (busyTrafficLevel == null || busyTrafficLevel.isBlank()) ? "similar" : busyTrafficLevel;
    }

    public String serviceStageOrDefault() {
        return (serviceStage == null || serviceStage.isBlank()) ? "mvp" : serviceStage;
    }

    public String serviceTypeOrDefault() {
        return (serviceType == null || serviceType.isBlank()) ? "web_api" : serviceType;
    }

    public String serviceDescriptionOrDefault() {
        return (serviceDescription == null || serviceDescription.isBlank()) ? "추가 요구사항 없음" : serviceDescription;
    }
}
