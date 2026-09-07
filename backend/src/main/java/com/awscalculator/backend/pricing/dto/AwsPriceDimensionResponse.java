package com.awscalculator.backend.pricing.dto;

import com.awscalculator.backend.pricing.AwsPriceDimension;
import java.math.BigDecimal;
import java.time.Instant;

public record AwsPriceDimensionResponse(
        Long id,
        String region,
        String priceKey,
        String optionKey,
        String unit,
        BigDecimal priceUsd,
        String source,
        Instant effectiveAt
) {
    public static AwsPriceDimensionResponse from(AwsPriceDimension dimension) {
        return new AwsPriceDimensionResponse(
                dimension.getId(),
                dimension.getRegion(),
                dimension.getPriceKey(),
                dimension.getOptionKey(),
                dimension.getUnit(),
                dimension.getPriceUsd(),
                dimension.getSource(),
                dimension.getEffectiveAt()
        );
    }
}
