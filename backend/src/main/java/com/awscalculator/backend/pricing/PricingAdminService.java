package com.awscalculator.backend.pricing;

import com.awscalculator.backend.pricing.dto.AwsPriceDimensionResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PricingAdminService {

    private final AwsPriceDimensionRepository priceDimensionRepository;

    @Transactional(readOnly = true)
    public List<AwsPriceDimensionResponse> listDimensions(String region) {
        List<AwsPriceDimension> dimensions = region == null || region.isBlank()
                ? priceDimensionRepository.findAll()
                : priceDimensionRepository.findByRegion(region);
        return dimensions.stream()
                .map(AwsPriceDimensionResponse::from)
                .toList();
    }
}
