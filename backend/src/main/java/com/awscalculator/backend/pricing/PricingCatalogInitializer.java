package com.awscalculator.backend.pricing;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PricingCatalogInitializer implements ApplicationRunner {

    private final PricingService pricingService;

    @Override
    public void run(ApplicationArguments args) {
        pricingService.seedFromSnapshotsIfMissing();
    }
}
