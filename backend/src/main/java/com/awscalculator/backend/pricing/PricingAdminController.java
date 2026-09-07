package com.awscalculator.backend.pricing;

import com.awscalculator.backend.pricing.dto.AwsPriceDimensionResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pricing")
@RequiredArgsConstructor
public class PricingAdminController {

    private final PricingAdminService pricingAdminService;

    @GetMapping("/dimensions")
    public List<AwsPriceDimensionResponse> listDimensions(@RequestParam(required = false) String region) {
        return pricingAdminService.listDimensions(region);
    }
}
