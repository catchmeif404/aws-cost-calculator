package com.awscalculator.backend.calculation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalculationRepository extends JpaRepository<Calculation, Long> {
    List<Calculation> findByProjectUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Calculation> findTopByProjectIdOrderByCreatedAtDesc(Long projectId);
}
