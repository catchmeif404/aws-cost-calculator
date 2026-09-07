package com.awscalculator.backend.project;

import com.awscalculator.backend.calculation.CalculationService;
import com.awscalculator.backend.calculation.dto.ProjectHistoryResponse;
import com.awscalculator.backend.project.dto.CreateProjectRequest;
import com.awscalculator.backend.project.dto.ProjectResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final CalculationService calculationService;

    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        Project project = projectService.create(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectResponse.from(project));
    }

    @GetMapping
    public List<ProjectHistoryResponse> listMine(@AuthenticationPrincipal Long userId) {
        return calculationService.listForUser(userId);
    }

    @GetMapping("/{id}")
    public ProjectResponse get(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        return ProjectResponse.from(projectService.getOwnedOrThrow(id, userId));
    }
}
