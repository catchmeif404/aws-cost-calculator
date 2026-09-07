package com.awscalculator.backend.resource.catalog;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "resource_catalog_field_options",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_resource_catalog_field_options_lookup",
                columnNames = {"catalog_entry_id", "field_key", "option_order"}
        )
)
@Getter
@NoArgsConstructor
public class ResourceCatalogFieldOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 옵션이 속한 리소스 카탈로그 항목.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catalog_entry_id", nullable = false)
    private ResourceCatalogEntry catalogEntry;

    // 옵션이 속한 필드 key.
    @Column(name = "field_key", nullable = false, length = 80)
    private String fieldKey;

    // 화면에 표시할 옵션 이름.
    @Column(nullable = false, length = 120)
    private String label;

    // 실제 configuration에 저장될 옵션 값.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "option_value", columnDefinition = "jsonb", nullable = false)
    private Object value;

    // 같은 필드 안에서 옵션이 표시되는 순서.
    @Column(name = "option_order", nullable = false)
    private int optionOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ResourceCatalogFieldOption(
            ResourceCatalogEntry catalogEntry,
            String fieldKey,
            String label,
            Object value,
            int optionOrder
    ) {
        this.catalogEntry = catalogEntry;
        this.fieldKey = fieldKey;
        this.label = label;
        this.value = value;
        this.optionOrder = optionOrder;
    }
}
