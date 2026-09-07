package com.awscalculator.backend.calculation;

import com.awscalculator.backend.resource.ResourceType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// A per-resource cost line, frozen at the moment a Calculation was saved — not a live join to
// Resource, so the history report keeps showing what a resource cost back then even if its
// configuration is edited (or the resource is removed) afterward.
@Entity
@Table(name = "calculation_resource_costs")
@Getter
@Setter
@NoArgsConstructor
public class CalculationResourceCost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calculation_id", nullable = false)
    private Calculation calculation;

    @Column(name = "resource_id")
    private Long resourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceType type;

    @Column(name = "monthly_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyCost;

    public CalculationResourceCost(Calculation calculation, Long resourceId, ResourceType type, BigDecimal monthlyCost) {
        this.calculation = calculation;
        this.resourceId = resourceId;
        this.type = type;
        this.monthlyCost = monthlyCost;
    }
}
