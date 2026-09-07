package com.awscalculator.backend.calculation;

import com.awscalculator.backend.resource.ResourceType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourcePricingRuleRepository extends JpaRepository<ResourcePricingRule, Long> {
    boolean existsByResourceTypeAndComponentName(ResourceType resourceType, String componentName);

    Optional<ResourcePricingRule> findByResourceTypeAndComponentName(ResourceType resourceType, String componentName);

    List<ResourcePricingRule> findAllByResourceTypeOrderByRuleOrderAsc(ResourceType resourceType);
}
