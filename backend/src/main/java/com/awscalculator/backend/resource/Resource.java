package com.awscalculator.backend.resource;

import com.awscalculator.backend.project.Project;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "resources")
@Getter
@Setter
@NoArgsConstructor
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // EAGER: CostEngine reads project.getRegion() for every resource it prices, so this is
    // needed outside a transaction (e.g. the AI explanation call, which shouldn't hold a DB
    // transaction open for the seconds-to-tens-of-seconds a codex-cli call can take).
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> configuration;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Resource(Project project, ResourceType type, Map<String, Object> configuration) {
        this.project = project;
        this.type = type;
        this.configuration = configuration;
    }
}
