package com.awscalculator.backend.project;

import com.awscalculator.backend.auth.UserRepository;
import com.awscalculator.backend.common.NotFoundException;
import com.awscalculator.backend.project.dto.CreateProjectRequest;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public Project create(CreateProjectRequest request, Long userId) {
        Project project = new Project(
                request.name(),
                request.regionOrDefault(),
                request.monthlyUsersOrDefault(),
                request.requestsPerUserOrDefault(),
                request.busyTrafficLevelOrDefault(),
                request.serviceStageOrDefault()
        );
        project.setUser(userRepository.getReferenceById(userId));
        return projectRepository.save(project);
    }

    // Internal use only (e.g. resolving a project inside an already-ownership-checked flow) —
    // every controller-facing lookup must go through getOwnedOrThrow instead.
    public Project getOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
    }

    // 404s (not 403) for someone else's project, same as CalculationService.getSavedCalculation —
    // this endpoint family shouldn't confirm whether a given project ID even exists to a caller
    // who doesn't own it. Project.user is nullable for rows created before that column existed
    // (see Project), so a null owner never matches any real, authenticated userId.
    public Project getOwnedOrThrow(Long projectId, Long userId) {
        Project project = getOrThrow(projectId);
        Long ownerId = project.getUser() == null ? null : project.getUser().getId();
        if (userId == null || !Objects.equals(ownerId, userId)) {
            throw new NotFoundException("Project not found: " + projectId);
        }
        return project;
    }
}
