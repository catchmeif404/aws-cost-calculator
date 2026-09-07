package com.awscalculator.backend.ai;

import com.awscalculator.backend.calculation.dto.ResourceCostItem;
import java.math.BigDecimal;
import java.util.Comparator;
import org.springframework.stereotype.Component;

/**
 * Deterministic, no-network fallback explainer. Always available, used whenever the primary
 * (AI-backed) explainer is disabled or fails — the user always gets some explanation.
 */
@Component
public class RuleBasedAiExplainer implements AiExplainer {

    @Override
    public String name() {
        return "rule-based";
    }

    @Override
    public String explain(ExplanationContext context) {
        StringBuilder sb = new StringBuilder();

        ResourceCostItem biggest = context.resources().stream()
                .max(Comparator.comparing(ResourceCostItem::monthlyCost))
                .orElse(null);

        long monthlyRequests = (long) context.monthlyUsers() * context.requestsPerUser();
        sb.append("월 ").append(monthlyRequests).append("건 정도의 요청을 기준으로, 이번 달 예상 비용은 $")
                .append(context.totalMonthlyCostUsd()).append("이에요.");
        if (biggest != null) {
            sb.append(" 가장 비중이 큰 항목은 ").append(biggest.type())
                    .append("(월 $").append(biggest.monthlyCost()).append(")예요.");
        }

        if (context.estimatedSavingsUsd() != null
                && context.estimatedSavingsUsd().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(" 아래 최적화 제안을 적용하면 약 $").append(context.estimatedSavingsUsd())
                    .append("/월을 줄일 수 있어요.");
        }

        return sb.toString();
    }
}
