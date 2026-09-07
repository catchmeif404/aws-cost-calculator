package com.awscalculator.backend.calculation;

import com.awscalculator.backend.resource.ResourceType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
        name = "resource_pricing_rules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_resource_pricing_rules_lookup",
                columnNames = {"resource_type", "component_name"}
        )
)
@Getter
@NoArgsConstructor
public class ResourcePricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 규칙이 적용되는 리소스 타입.
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 60)
    private ResourceType resourceType;

    // 같은 리소스 안에서 비용 항목을 구분하는 이름.
    @Column(name = "component_name", nullable = false, length = 80)
    private String componentName;

    // 백엔드가 지원하는 제한된 계산 공식 타입.
    @Enumerated(EnumType.STRING)
    @Column(name = "formula_type", nullable = false, length = 60)
    private ResourcePricingFormulaType formulaType;

    // aws_price_dimensions.price_key와 매칭되는 단가 키.
    @Column(name = "price_key", nullable = false, length = 100)
    private String priceKey;

    // option 단가일 때 resources.configuration에서 option_key로 읽을 필드.
    @Column(name = "option_field", length = 80)
    private String optionField;

    // 첫 번째 수량 필드. 비어 있으면 1로 처리한다.
    @Column(name = "quantity_field", length = 80)
    private String quantityField;

    // 두 번째 수량 필드. 비어 있으면 1로 처리한다.
    @Column(name = "quantity_field_2", length = 80)
    private String quantityField2;

    // 세 번째 수량 필드. 비어 있으면 1로 처리한다.
    @Column(name = "quantity_field_3", length = 80)
    private String quantityField3;

    // 모든 수량에 추가로 곱할 고정 계수.
    @Column(nullable = false, precision = 18, scale = 10)
    private BigDecimal multiplier = BigDecimal.ONE;

    // 월 시간 수를 추가로 곱할지 여부.
    @Column(name = "multiply_hours", nullable = false)
    private boolean multiplyHours;

    // 특정 configuration 값에서만 적용할 때 사용할 필드.
    @Column(name = "condition_field", length = 80)
    private String conditionField;

    // condition_field가 이 값과 같을 때만 규칙을 적용한다.
    @Column(name = "condition_value", length = 120)
    private String conditionValue;

    // 특정 configuration 값이면 규칙을 제외할 때 사용할 필드.
    @Column(name = "disabled_when_field", length = 80)
    private String disabledWhenField;

    // disabled_when_field가 이 값과 같으면 규칙을 제외한다.
    @Column(name = "disabled_when_value", length = 120)
    private String disabledWhenValue;

    // 같은 리소스 타입 안에서 규칙을 적용하는 순서.
    @Column(name = "rule_order", nullable = false)
    private int ruleOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ResourcePricingRule(
            ResourceType resourceType,
            String componentName,
            ResourcePricingFormulaType formulaType,
            String priceKey,
            String optionField,
            String quantityField,
            String quantityField2,
            String quantityField3,
            BigDecimal multiplier,
            boolean multiplyHours,
            String conditionField,
            String conditionValue,
            String disabledWhenField,
            String disabledWhenValue,
            int ruleOrder
    ) {
        this.resourceType = resourceType;
        this.componentName = componentName;
        this.formulaType = formulaType;
        this.priceKey = priceKey;
        this.optionField = optionField;
        this.quantityField = quantityField;
        this.quantityField2 = quantityField2;
        this.quantityField3 = quantityField3;
        this.multiplier = multiplier == null ? BigDecimal.ONE : multiplier;
        this.multiplyHours = multiplyHours;
        this.conditionField = conditionField;
        this.conditionValue = conditionValue;
        this.disabledWhenField = disabledWhenField;
        this.disabledWhenValue = disabledWhenValue;
        this.ruleOrder = ruleOrder;
    }

    public void updateFrom(ResourcePricingRule rule) {
        this.formulaType = rule.formulaType;
        this.priceKey = rule.priceKey;
        this.optionField = rule.optionField;
        this.quantityField = rule.quantityField;
        this.quantityField2 = rule.quantityField2;
        this.quantityField3 = rule.quantityField3;
        this.multiplier = rule.multiplier;
        this.multiplyHours = rule.multiplyHours;
        this.conditionField = rule.conditionField;
        this.conditionValue = rule.conditionValue;
        this.disabledWhenField = rule.disabledWhenField;
        this.disabledWhenValue = rule.disabledWhenValue;
        this.ruleOrder = rule.ruleOrder;
    }
}
