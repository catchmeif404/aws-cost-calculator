package com.awscalculator.backend.optimization;

import com.awscalculator.backend.resource.Resource;
import java.math.BigDecimal;

final class CostOptimizationPromptBuilder {

    private CostOptimizationPromptBuilder() {
    }

    static String build(CostOptimizationContext context) {
        return "en".equals(context.locale()) ? buildEnglish(context) : buildKorean(context);
    }

    private static String buildKorean(CostOptimizationContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("너는 AWS 비용 최적화 컨설턴트야. ")
                .append("아래는 실제로 서버에서 계산된 AWS 리소스 구성과 월 비용이야. ")
                .append("이 구성에 실제로 적용 가능한, 구체적인 비용 절감 방법을 한국어로 최대 5개까지 제안해줘. ")
                .append("일반론적인 이야기(\"모니터링을 하세요\" 같은)는 피하고, 주어진 리소스 타입과 설정값을 근거로 삼아. ")
                .append("가능하면 Graviton 전환(~20% 절감), Reserved Instance/Savings Plans(~30~40% 절감), ")
                .append("트래픽 대비 과다 스펙 다운사이징 같은 현실적인 AWS 할인율을 참고해서 절감액을 추정하고, ")
                .append("합리적으로 추정할 수 없으면 estimatedMonthlySavingsUsd를 null로 둬. ")
                .append("절대 다른 텍스트 없이 아래 JSON 형식으로만 응답해:\n")
                .append("{\"suggestions\":[{\"message\":\"...\",\"estimatedMonthlySavingsUsd\":number|null}]}\n\n");

        sb.append("서비스 규모:\n");
        sb.append("- 월간 사용자 수: ").append(context.project().getMonthlyUsers()).append("\n");
        sb.append("- 사용자당 평균 요청 수: ").append(context.project().getRequestsPerUser()).append("\n");
        sb.append("- 가장 바쁜 시간대의 트래픽 수준: ").append(context.project().getBusyTrafficLevel()).append("\n");
        sb.append("- 서비스 운영 단계: ").append(context.project().getServiceStage()).append("\n\n");

        sb.append("현재 리소스 구성 (총 월 $").append(context.totalMonthlyCostUsd()).append("):\n");
        for (Resource resource : context.resources()) {
            BigDecimal cost = context.monthlyCostByResourceId().get(resource.getId());
            sb.append("- ").append(resource.getType())
                    .append(": $").append(cost).append("/월, 설정=")
                    .append(resource.getConfiguration())
                    .append("\n");
        }

        return sb.toString();
    }

    private static String buildEnglish(CostOptimizationContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an AWS cost optimization consultant. ")
                .append("Below is an AWS resource configuration and its monthly cost, already computed on the server. ")
                .append("Suggest up to 5 concrete, actually-applicable cost-saving measures for this configuration, in English. ")
                .append("Avoid generic advice (like \"set up monitoring\") — ground every suggestion in the given resource types and settings. ")
                .append("Where relevant, estimate savings using realistic AWS discount rates such as Graviton migration (~20% savings), ")
                .append("Reserved Instances/Savings Plans (~30-40% savings), or downsizing over-provisioned resources relative to traffic, ")
                .append("and set estimatedMonthlySavingsUsd to null when it can't be reasonably estimated. ")
                .append("Respond with ONLY the following JSON shape, no other text:\n")
                .append("{\"suggestions\":[{\"message\":\"...\",\"estimatedMonthlySavingsUsd\":number|null}]}\n\n");

        sb.append("Service scale:\n");
        sb.append("- Monthly active users: ").append(context.project().getMonthlyUsers()).append("\n");
        sb.append("- Average requests per user: ").append(context.project().getRequestsPerUser()).append("\n");
        sb.append("- Traffic level at peak hours: ").append(context.project().getBusyTrafficLevel()).append("\n");
        sb.append("- Service stage: ").append(context.project().getServiceStage()).append("\n\n");

        sb.append("Current resource configuration (total $").append(context.totalMonthlyCostUsd()).append("/month):\n");
        for (Resource resource : context.resources()) {
            BigDecimal cost = context.monthlyCostByResourceId().get(resource.getId());
            sb.append("- ").append(resource.getType())
                    .append(": $").append(cost).append("/month, config=")
                    .append(resource.getConfiguration())
                    .append("\n");
        }

        return sb.toString();
    }
}
