package com.awscalculator.backend.pricing;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AwsPriceDimensionRepository extends JpaRepository<AwsPriceDimension, Long> {
    Optional<AwsPriceDimension> findByRegionAndPriceKeyAndOptionKey(String region, String priceKey, String optionKey);

    void deleteByRegionAndPriceKeyAndOptionKey(String region, String priceKey, String optionKey);

    List<AwsPriceDimension> findByRegion(String region);
}
