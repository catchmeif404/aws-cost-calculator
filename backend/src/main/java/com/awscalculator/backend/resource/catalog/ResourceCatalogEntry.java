package com.awscalculator.backend.resource.catalog;

import com.awscalculator.backend.resource.ResourceType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// DB-backed replacement for the old hardcoded ResourceCatalog.java list. `defaults`/`fields` use
// the same @JdbcTypeCode(SqlTypes.JSON) + jsonb pattern already proven by Resource.configuration
// (resource/Resource.java) — no custom AttributeConverter needed, Hibernate handles it natively.
@Entity
@Table(name = "resource_catalog_entries")
@Getter
@Setter
@NoArgsConstructor
public class ResourceCatalogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private ResourceType type;

    @Column(nullable = false)
    private String kind;

    @Column(nullable = false)
    private String title;

    @Column(name = "short_name", nullable = false)
    private String shortName;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String tone;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> defaults;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<ResourceCatalogField> fields;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ResourceCatalogEntry(ResourceType type, String kind, String title, String shortName, String category,
                                 String description, String tone, Map<String, Object> defaults,
                                 List<ResourceCatalogField> fields) {
        this.type = type;
        this.kind = kind;
        this.title = title;
        this.shortName = shortName;
        this.category = category;
        this.description = description;
        this.tone = tone;
        this.defaults = defaults;
        this.fields = fields;
    }
}
