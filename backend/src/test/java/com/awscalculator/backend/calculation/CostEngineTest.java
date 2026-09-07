package com.awscalculator.backend.calculation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.awscalculator.backend.pricing.AwsPriceDimension;
import com.awscalculator.backend.pricing.PricingDimensionIndex;
import com.awscalculator.backend.pricing.PricingService;
import com.awscalculator.backend.resource.Resource;
import com.awscalculator.backend.resource.ResourceType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CostEngineTest {

    @Mock
    private PricingService pricingService;

    @Mock
    private ResourcePricingRuleRepository resourcePricingRuleRepository;

    @InjectMocks
    private CostEngine costEngine;

    @Test
    void monthlyCostUsesConditionalEcsArmRules() {
        when(pricingService.dimensionsForRegion("ap-northeast-2")).thenReturn(pricing(
                Map.of(
                        "fargate_vcpu_hour_arm", scalar("0.04"),
                        "fargate_gb_hour_arm", scalar("0.005")
                )
        ));
        when(resourcePricingRuleRepository.findAllByResourceTypeOrderByRuleOrderAsc(ResourceType.ECS))
                .thenReturn(List.of(
                        rule(ResourceType.ECS, "vcpu-arm", "fargate_vcpu_hour_arm", null, "cpu", "tasks", null,
                                true, "architecture", "arm64", 10),
                        rule(ResourceType.ECS, "memory-arm", "fargate_gb_hour_arm", null, "memory", "tasks", null,
                                true, "architecture", "arm64", 20)
                ));
        Resource resource = new Resource(null, ResourceType.ECS,
                Map.of("cpu", 1, "memory", 2, "tasks", 3, "architecture", "arm64"));

        BigDecimal cost = costEngine.monthlyCost(resource, "ap-northeast-2");

        assertThat(cost).isEqualByComparingTo("109.50");
    }

    @Test
    void monthlyCostUsesSelectedOptionAsPriceDimensionOptionKey() {
        when(pricingService.dimensionsForRegion("ap-northeast-2")).thenReturn(pricing(
                Map.of(
                        "rds_instance_hour", Map.of("db.t4g.micro", new BigDecimal("0.025")),
                        "rds_storage_gb_month", scalar("0.131")
                )
        ));
        when(resourcePricingRuleRepository.findAllByResourceTypeOrderByRuleOrderAsc(ResourceType.RDS))
                .thenReturn(List.of(
                        rule(ResourceType.RDS, "instance", "rds_instance_hour", "instanceType", null, null, null,
                                true, null, null, 10),
                        rule(ResourceType.RDS, "storage", "rds_storage_gb_month", null, "storageGb", null, null,
                                false, null, null, 20)
                ));
        Resource resource = new Resource(null, ResourceType.RDS,
                Map.of("instanceType", "db.t4g.micro", "storageGb", 20));

        BigDecimal cost = costEngine.monthlyCost(resource, "ap-northeast-2");

        assertThat(cost).isEqualByComparingTo("20.87");
    }

    @Test
    void monthlyCostUsesDetailedOptionForNewResourceTypes() {
        when(pricingService.dimensionsForRegion("ap-northeast-2")).thenReturn(pricing(
                Map.of(
                        "eks_cluster_hour", Map.of(
                                "standard", new BigDecimal("0.1"),
                                "extended", new BigDecimal("0.5")
                        )
                )
        ));
        when(resourcePricingRuleRepository.findAllByResourceTypeOrderByRuleOrderAsc(ResourceType.EKS))
                .thenReturn(List.of(
                        rule(ResourceType.EKS, "cluster", "eks_cluster_hour", "controlPlaneTier",
                                "clusterCount", null, null, true, null, null, 10)
                ));
        Resource resource = new Resource(null, ResourceType.EKS,
                Map.of("controlPlaneTier", "extended", "clusterCount", 2));

        BigDecimal cost = costEngine.monthlyCost(resource, "ap-northeast-2");

        assertThat(cost).isEqualByComparingTo("730.00");
    }

    @Test
    void monthlyCostAddsSesRecipientAndAttachmentRules() {
        when(pricingService.dimensionsForRegion("ap-northeast-2")).thenReturn(pricing(
                Map.of(
                        "ses_per_recipient", scalar("0.0001"),
                        "ses_attachment_gb", scalar("0.12")
                )
        ));
        when(resourcePricingRuleRepository.findAllByResourceTypeOrderByRuleOrderAsc(ResourceType.SES))
                .thenReturn(List.of(
                        rule(ResourceType.SES, "recipient", "ses_per_recipient", null,
                                "recipientsPerMonth", null, null, false, null, null, 10),
                        rule(ResourceType.SES, "attachment", "ses_attachment_gb", null,
                                "attachmentGb", null, null, false, null, null, 20)
                ));
        Resource resource = new Resource(null, ResourceType.SES,
                Map.of("recipientsPerMonth", 100_000, "attachmentGb", 10));

        BigDecimal cost = costEngine.monthlyCost(resource, "ap-northeast-2");

        assertThat(cost).isEqualByComparingTo("11.20");
    }

    private PricingDimensionIndex pricing(Map<String, Map<String, BigDecimal>> prices) {
        return new PricingDimensionIndex(
                "ap-northeast-2",
                new BigDecimal("730"),
                new BigDecimal("1380"),
                prices
        );
    }

    private Map<String, BigDecimal> scalar(String value) {
        return Map.of(AwsPriceDimension.DEFAULT_OPTION, new BigDecimal(value));
    }

    private ResourcePricingRule rule(
            ResourceType resourceType,
            String componentName,
            String priceKey,
            String optionField,
            String quantityField,
            String quantityField2,
            String quantityField3,
            boolean multiplyHours,
            String conditionField,
            String conditionValue,
            int ruleOrder
    ) {
        return new ResourcePricingRule(
                resourceType,
                componentName,
                ResourcePricingFormulaType.PRICE_TIMES_FACTORS,
                priceKey,
                optionField,
                quantityField,
                quantityField2,
                quantityField3,
                BigDecimal.ONE,
                multiplyHours,
                conditionField,
                conditionValue,
                null,
                null,
                ruleOrder
        );
    }
}
