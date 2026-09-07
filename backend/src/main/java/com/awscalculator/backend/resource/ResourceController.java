package com.awscalculator.backend.resource;

import com.awscalculator.backend.resource.dto.AddResourceRequest;
import com.awscalculator.backend.resource.dto.ResourceResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping
    public ResponseEntity<ResourceResponse> add(
            @PathVariable Long projectId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AddResourceRequest request
    ) {
        Resource resource = resourceService.add(projectId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResourceResponse.from(resource));
    }

    @GetMapping
    public List<ResourceResponse> list(@PathVariable Long projectId, @AuthenticationPrincipal Long userId) {
        return resourceService.listByProject(projectId, userId).stream().map(ResourceResponse::from).toList();
    }
}
