package com.awscalculator.backend.project.dto;

import com.awscalculator.backend.project.Project;
import java.time.Instant;

public record ProjectResponse(
        Long id,
        String name,
        String region,
        int monthlyUsers,
        int requestsPerUser,
        String busyTrafficLevel,
        String serviceStage,
        Instant createdAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getRegion(),
                project.getMonthlyUsers(),
                project.getRequestsPerUser(),
                project.getBusyTrafficLevel(),
                project.getServiceStage(),
                project.getCreatedAt()
        );
    }
}
