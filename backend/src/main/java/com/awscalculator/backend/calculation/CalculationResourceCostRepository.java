package com.awscalculator.backend.calculation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalculationResourceCostRepository extends JpaRepository<CalculationResourceCost, Long> {
    List<CalculationResourceCost> findByCalculationIdOrderByIdAsc(Long calculationId);
}
