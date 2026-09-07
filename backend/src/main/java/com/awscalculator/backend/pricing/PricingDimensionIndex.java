package com.awscalculator.backend.pricing;

import static com.awscalculator.backend.pricing.AwsPriceDimension.DEFAULT_OPTION;

import java.math.BigDecimal;
import java.util.Map;

public record PricingDimensionIndex(
        String region,
        BigDecimal hoursPerMonth,
        BigDecimal usdToKrw,
        Map<String, Map<String, BigDecimal>> prices
) {
    public BigDecimal scalar(String priceKey) {
        BigDecimal value = prices.getOrDefault(priceKey, Map.of()).get(DEFAULT_OPTION);
        if (value == null) {
            throw new IllegalStateException("Missing price dimension: " + priceKey);
        }
        return value;
    }

    public BigDecimal option(String priceKey, String optionKey) {
        BigDecimal value = prices.getOrDefault(priceKey, Map.of()).get(optionKey);
        if (value == null) {
            throw new IllegalArgumentException("Unknown price dimension option: " + priceKey + "/" + optionKey);
        }
        return value;
    }
}
