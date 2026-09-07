package com.awscalculator.backend.pricing;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
        name = "aws_price_dimensions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_aws_price_dimensions_lookup",
                columnNames = {"region", "price_key", "option_key"}
        )
)
@Getter
@NoArgsConstructor
public class AwsPriceDimension {

    public static final String DEFAULT_OPTION = "__default";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // AWS 리전 코드. 예: ap-northeast-2.
    @Column(nullable = false, length = 40)
    private String region;

    // 계산 엔진이 참조하는 가격 항목 키.
    @Column(name = "price_key", nullable = false, length = 80)
    private String priceKey;

    // 인스턴스 타입처럼 같은 priceKey 안에서 세분화되는 옵션 키.
    @Column(name = "option_key", nullable = false, length = 120)
    private String optionKey = DEFAULT_OPTION;

    // 과금 단위. 예: hour, gb_month, request.
    @Column(nullable = false, length = 40)
    private String unit;

    // USD 기준 단가.
    @Column(name = "price_usd", nullable = false, precision = 18, scale = 10)
    private BigDecimal priceUsd;

    // 가격 원본 식별자. 현재는 JSON seed snapshot 경로를 기록한다.
    @Column(length = 300)
    private String source;

    // 가격 데이터 기준 시각.
    @Column(name = "effective_at")
    private Instant effectiveAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AwsPriceDimension(
            String region,
            String priceKey,
            String optionKey,
            String unit,
            BigDecimal priceUsd,
            String source,
            Instant effectiveAt
    ) {
        this.region = region;
        this.priceKey = priceKey;
        this.optionKey = optionKey == null || optionKey.isBlank() ? DEFAULT_OPTION : optionKey;
        this.unit = unit;
        this.priceUsd = priceUsd;
        this.source = source;
        this.effectiveAt = effectiveAt;
    }

    public void updatePrice(String unit, BigDecimal priceUsd, String source, Instant effectiveAt) {
        this.unit = unit;
        this.priceUsd = priceUsd;
        this.source = source;
        this.effectiveAt = effectiveAt;
    }
}
