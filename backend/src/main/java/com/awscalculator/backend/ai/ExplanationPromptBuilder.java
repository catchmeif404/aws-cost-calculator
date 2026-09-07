package com.awscalculator.backend.ai;

import com.awscalculator.backend.calculation.dto.ResourceCostItem;
import com.awscalculator.backend.optimization.dto.Suggestion;

final class ExplanationPromptBuilder {

    private ExplanationPromptBuilder() {
    }

    static String build(ExplanationContext context) {
        boolean english = "en".equals(context.locale());
        return english ? buildEnglish(context) : buildKorean(context);
    }

    private static String buildKorean(ExplanationContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("너는 AWS 비용 계산기 서비스의 설명 도우미야. ")
                .append("아래는 이미 서버에서 정확하게 계산된 AWS 월간 비용 데이터야. ")
                .append("이 숫자를 그대로 인용해서, 왜 비용이 이렇게 나왔는지와 어떻게 줄일 수 있는지 ")
                .append("한국어로 3~4문장 이내로 친근하게 설명해줘. ")
                .append("절대로 숫자를 새로 계산하거나 바꾸지 마. 주어진 숫자만 그대로 사용해. ")
                .append("코드나 커맨드는 실행하지 말고, 순수 텍스트 설명만 답해.\n\n");

        sb.append("서비스 규모:\n");
        sb.append("- 월간 사용자 수: ").append(context.monthlyUsers()).append("\n");
        sb.append("- 사용자당 평균 요청 수: ").append(context.requestsPerUser()).append("\n");
        sb.append("- 가장 바쁜 시간대의 트래픽 수준: ").append(labelBusyTrafficLevelKo(context.busyTrafficLevel())).append("\n");
        sb.append("- 서비스 운영 단계: ").append(labelServiceStageKo(context.serviceStage())).append("\n\n");

        sb.append("월 총 예상 비용: $").append(context.totalMonthlyCostUsd()).append("\n");
        sb.append("리소스별 비용:\n");
        for (ResourceCostItem item : context.resources()) {
            sb.append("- ").append(item.type()).append(": $").append(item.monthlyCost()).append("\n");
        }

        if (!context.suggestions().isEmpty()) {
            sb.append("\n절감 제안 (이미 계산된 예상 절감액 포함):\n");
            for (Suggestion s : context.suggestions()) {
                sb.append("- ").append(s.message());
                if (s.estimatedMonthlySavingsUsd() != null) {
                    sb.append(" (약 $").append(s.estimatedMonthlySavingsUsd()).append("/월 절감)");
                }
                sb.append("\n");
            }
            sb.append("\n적용 시 총 예상 절감액: $").append(context.estimatedSavingsUsd()).append("\n");
        }

        return sb.toString();
    }

    private static String buildEnglish(ExplanationContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are the explanation assistant for an AWS cost calculator service. ")
                .append("Below is AWS monthly cost data already computed precisely on the server. ")
                .append("Quoting these numbers exactly as given, explain in English, in 3-4 friendly sentences, ")
                .append("why the cost came out this way and how it could be reduced. ")
                .append("Never recompute or change any number — use only the numbers given. ")
                .append("Do not run any code or commands, reply with plain text explanation only.\n\n");

        sb.append("Service scale:\n");
        sb.append("- Monthly active users: ").append(context.monthlyUsers()).append("\n");
        sb.append("- Average requests per user: ").append(context.requestsPerUser()).append("\n");
        sb.append("- Traffic level at peak hours: ").append(labelBusyTrafficLevelEn(context.busyTrafficLevel())).append("\n");
        sb.append("- Service stage: ").append(labelServiceStageEn(context.serviceStage())).append("\n\n");

        sb.append("Total estimated monthly cost: $").append(context.totalMonthlyCostUsd()).append("\n");
        sb.append("Cost by resource:\n");
        for (ResourceCostItem item : context.resources()) {
            sb.append("- ").append(item.type()).append(": $").append(item.monthlyCost()).append("\n");
        }

        if (!context.suggestions().isEmpty()) {
            sb.append("\nSavings suggestions (with already-computed estimated savings):\n");
            for (Suggestion s : context.suggestions()) {
                sb.append("- ").append(s.message());
                if (s.estimatedMonthlySavingsUsd() != null) {
                    sb.append(" (about $").append(s.estimatedMonthlySavingsUsd()).append("/month savings)");
                }
                sb.append("\n");
            }
            sb.append("\nTotal estimated savings if applied: $").append(context.estimatedSavingsUsd()).append("\n");
        }

        return sb.toString();
    }

    private static String labelBusyTrafficLevelKo(String value) {
        return switch (value) {
            case "two_to_three_times" -> "평소보다 2~3배 많음";
            case "five_plus_times" -> "평소보다 5배 이상 많음";
            case "unknown" -> "잘 모르겠음";
            default -> "평소와 비슷함";
        };
    }

    private static String labelServiceStageKo(String value) {
        return switch (value) {
            case "toy" -> "개인/토이 프로젝트";
            case "production" -> "실제 운영 서비스";
            case "critical" -> "장애에 민감한 서비스";
            default -> "MVP/초기 서비스";
        };
    }

    private static String labelBusyTrafficLevelEn(String value) {
        return switch (value) {
            case "two_to_three_times" -> "2-3x higher than usual";
            case "five_plus_times" -> "5x+ higher than usual";
            case "unknown" -> "not sure";
            default -> "about the same as usual";
        };
    }

    private static String labelServiceStageEn(String value) {
        return switch (value) {
            case "toy" -> "personal/toy project";
            case "production" -> "production service";
            case "critical" -> "mission-critical service";
            default -> "MVP/early-stage service";
        };
    }
}
