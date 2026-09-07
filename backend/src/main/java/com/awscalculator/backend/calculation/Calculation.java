package com.awscalculator.backend.calculation;

import com.awscalculator.backend.project.Project;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "calculations")
@Getter
@Setter
@NoArgsConstructor
public class Calculation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "total_monthly_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalMonthlyCost;

    // Nullable: added after this table already had rows (see Project.user for the same reasoning)
    // and only ever read back through the user-scoped history list, whose rows are all new saves.
    @Column(name = "total_monthly_cost_krw", precision = 14, scale = 0)
    private BigDecimal totalMonthlyCostKrw;

    @Column(nullable = false)
    private String currency;

    // Opaque pass-through — see CalculationResponse.diagramSnapshot. Null when calculated from the
    // wizard flow (which never has a drag-builder diagram to begin with).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "diagram_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> diagramSnapshot;

    // Opaque pass-through — see CalculationResponse.recommendationMetadata. Null unless this
    // calculation's resources came from an applied AI architecture recommendation.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recommendation_metadata", columnDefinition = "jsonb")
    private Map<String, Object> recommendationMetadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Calculation(
            Project project,
            BigDecimal totalMonthlyCost,
            BigDecimal totalMonthlyCostKrw,
            String currency,
            Map<String, Object> diagramSnapshot,
            Map<String, Object> recommendationMetadata
    ) {
        this.project = project;
        this.totalMonthlyCost = totalMonthlyCost;
        this.totalMonthlyCostKrw = totalMonthlyCostKrw;
        this.currency = currency;
        this.diagramSnapshot = diagramSnapshot;
        this.recommendationMetadata = recommendationMetadata;
    }
}
