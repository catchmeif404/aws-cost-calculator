package com.awscalculator.backend.resource.catalog;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resource-catalog")
@RequiredArgsConstructor
public class ResourceCatalogController {

    private final ResourceCatalogService resourceCatalogService;

    @GetMapping
    public List<ResourceCatalogItem> list() {
        return resourceCatalogService.items();
    }
}
