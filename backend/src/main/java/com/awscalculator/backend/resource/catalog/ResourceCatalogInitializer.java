package com.awscalculator.backend.resource.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResourceCatalogInitializer implements ApplicationRunner {

    private final ResourceCatalogService resourceCatalogService;

    @Override
    public void run(ApplicationArguments args) {
        resourceCatalogService.seedIfMissing();
    }
}
