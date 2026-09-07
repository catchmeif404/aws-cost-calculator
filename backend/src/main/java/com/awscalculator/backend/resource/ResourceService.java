package com.awscalculator.backend.resource;

import com.awscalculator.backend.project.Project;
import com.awscalculator.backend.project.ProjectService;
import com.awscalculator.backend.resource.dto.AddResourceRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class ResourceService {

    // configuration ends up embedded verbatim in the AI cost-optimization prompt
    // (CostOptimizationPromptBuilder) — capping its serialized size keeps a single request from
    // ballooning into a huge, expensive LLM call regardless of how many credits it costs.
    private static final int MAX_CONFIGURATION_JSON_LENGTH = 4_000;

    private final ResourceRepository resourceRepository;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper;

    public Resource add(Long projectId, Long userId, AddResourceRequest request) {
        Project project = projectService.getOwnedOrThrow(projectId, userId);
        String json = objectMapper.writeValueAsString(request.configuration());
        if (json.length() > MAX_CONFIGURATION_JSON_LENGTH) {
            throw new IllegalArgumentException(
                    "Resource configuration is too large (max " + MAX_CONFIGURATION_JSON_LENGTH + " JSON characters)");
        }
        Resource resource = new Resource(project, request.type(), request.configuration());
        return resourceRepository.save(resource);
    }

    public List<Resource> listByProject(Long projectId, Long userId) {
        projectService.getOwnedOrThrow(projectId, userId);
        return resourceRepository.findByProjectId(projectId);
    }
}
