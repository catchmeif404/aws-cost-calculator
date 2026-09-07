package com.awscalculator.backend.recommendation;

import com.awscalculator.backend.recommendation.ArchitectureTier.TierResourceSpec;
import com.awscalculator.backend.recommendation.dto.AiResourceSpec;
import com.awscalculator.backend.recommendation.dto.ServiceRecommendationRequest;
import com.awscalculator.backend.resource.ResourceType;
import com.awscalculator.backend.resource.catalog.ResourceCatalogItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Deterministic fallback used when no primary (LLM-backed) advisor is configured, or when the
 * primary one fails or returns nothing usable: picks one of the fixed {@link ArchitectureTiers}
 * presets by service profile and nudges it with keyword-based signals from the free-text
 * description. Unlike the primary advisors this never calls out to an LLM, so it's what keeps the
 * "구성 추천" feature working even with no AI provider configured.
 */
@Component
public class RuleBasedArchitectureRecommendationAdvisor implements ArchitectureRecommendationAdvisor {

    @Override
    public String name() {
        return "rule-based";
    }

    @Override
    public ArchitectureRecommendationAdvice advise(
            ServiceRecommendationRequest request,
            long estimatedMonthlyRequests,
            List<ResourceCatalogItem> catalog,
            String locale
    ) {
        int tierIndex = selectTierIndex(estimatedMonthlyRequests, request.busyTrafficLevelOrDefault(), request.serviceStageOrDefault());
        ArchitectureTier tier = ArchitectureTiers.TIERS.get(tierIndex);
        List<TierResourceSpec> specs = new ArrayList<>(tier.resources());
        List<String> additionalRecommendations = new ArrayList<>();
        applyServiceSignals(request, estimatedMonthlyRequests, specs, additionalRecommendations);

        List<AiResourceSpec> resources = specs.stream()
                .map(spec -> new AiResourceSpec(spec.type().name(), spec.configuration()))
                .toList();

        String reason = "월 " + estimatedMonthlyRequests + "건 정도의 요청, "
                + labelBusyTrafficLevel(request.busyTrafficLevelOrDefault()) + ", "
                + labelServiceStage(request.serviceStageOrDefault()) + ", "
                + labelServiceType(request.serviceTypeOrDefault()) + " 특성을 함께 보고 골랐습니다.";

        return new ArchitectureRecommendationAdvice(
                tier.name(), tier.description(), resources, reason, additionalRecommendations, name(), false
        );
    }

    private void applyServiceSignals(
            ServiceRecommendationRequest request,
            long estimatedMonthlyRequests,
            List<TierResourceSpec> specs,
            List<String> additionalRecommendations
    ) {
        String serviceType = request.serviceTypeOrDefault();
        String description = request.serviceDescriptionOrDefault().toLowerCase();

        if (containsAny(description, "비동기", "큐", "queue", "sqs", "eventbridge", "알림", "이미지 처리", "배치")) {
            addIfMissing(specs, ResourceType.LAMBDA, Map.of(
                    "requestsPerMonth", Math.max(100_000L, estimatedMonthlyRequests / 10),
                    "avgDurationMs", 500,
                    "memoryMb", 512
            ));
        }
        if (containsAny(description, "파일", "업로드", "이미지", "첨부", "정적", "cdn", "캐시")) {
            addIfMissing(specs, ResourceType.S3, Map.of("storageGb", storageGbFor(estimatedMonthlyRequests)));
            addIfMissing(specs, ResourceType.CLOUDFRONT, Map.of("dataTransferGb", transferGbFor(estimatedMonthlyRequests)));
        }
        if (containsAny(description, "nosql", "dynamodb", "키-값", "key-value", "문서 db", "document db")) {
            removeType(specs, ResourceType.RDS);
            addIfMissing(specs, ResourceType.DYNAMODB, Map.of(
                    "monthlyReadRequests", Math.max(1_000_000L, estimatedMonthlyRequests),
                    "monthlyWriteRequests", Math.max(200_000L, estimatedMonthlyRequests / 5),
                    "storageGb", 10
            ));
        }
        if ("static_site".equals(serviceType)) {
            removeType(specs, ResourceType.ECS);
            removeType(specs, ResourceType.ALB);
            removeType(specs, ResourceType.RDS);
            addIfMissing(specs, ResourceType.S3, Map.of("storageGb", storageGbFor(estimatedMonthlyRequests)));
            addIfMissing(specs, ResourceType.CLOUDFRONT, Map.of("dataTransferGb", transferGbFor(estimatedMonthlyRequests)));
        }
        if ("mobile_app".equals(serviceType)) {
            addIfMissing(specs, ResourceType.CLOUDFRONT, Map.of("dataTransferGb", transferGbFor(estimatedMonthlyRequests)));
            additionalRecommendations.add("모바일 앱은 푸시 알림, 앱 배포, API 인증 정책을 별도 요구사항으로 분리해 산정하세요.");
        }
        if ("internal_admin".equals(serviceType)) {
            additionalRecommendations.add("사내 관리자 도구는 공개 트래픽보다 접근 제어, VPN/SSO, 감사 로그 비용을 우선 검토하세요.");
        }
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private void addIfMissing(List<TierResourceSpec> specs, ResourceType type, Map<String, Object> configuration) {
        boolean exists = specs.stream().anyMatch(spec -> spec.type() == type);
        if (!exists) {
            specs.add(new TierResourceSpec(type, configuration));
        }
    }

    private void removeType(List<TierResourceSpec> specs, ResourceType type) {
        specs.removeIf(spec -> spec.type() == type);
    }

    private int storageGbFor(long estimatedMonthlyRequests) {
        if (estimatedMonthlyRequests >= 10_000_000) {
            return 100;
        }
        if (estimatedMonthlyRequests >= 1_000_000) {
            return 50;
        }
        return 10;
    }

    private int transferGbFor(long estimatedMonthlyRequests) {
        if (estimatedMonthlyRequests >= 10_000_000) {
            return 300;
        }
        if (estimatedMonthlyRequests >= 1_000_000) {
            return 100;
        }
        return 30;
    }

    private int selectTierIndex(long monthlyRequests, String busyTrafficLevel, String serviceStage) {
        int score = 0;

        if (monthlyRequests >= 10_000_000) {
            score += 3;
        } else if (monthlyRequests >= 1_000_000) {
            score += 2;
        } else if (monthlyRequests >= 100_000) {
            score += 1;
        }

        score += switch (busyTrafficLevel) {
            case "five_plus_times" -> 2;
            case "two_to_three_times" -> 1;
            default -> 0;
        };

        score += switch (serviceStage) {
            case "critical" -> 2;
            case "production" -> 1;
            case "toy" -> -1;
            default -> 0;
        };

        if (score <= 0) {
            return 0;
        }
        if (score <= 2) {
            return 1;
        }
        if (score <= 4) {
            return 2;
        }
        return 3;
    }

    private String labelBusyTrafficLevel(String value) {
        return switch (value) {
            case "two_to_three_times" -> "가장 바쁜 시간대에는 평소보다 2~3배 많은 트래픽";
            case "five_plus_times" -> "가장 바쁜 시간대에는 평소보다 5배 이상 많은 트래픽";
            case "unknown" -> "시간대별 트래픽 변동은 알 수 없음";
            default -> "시간대별 트래픽 변동이 크지 않음";
        };
    }

    private String labelServiceStage(String value) {
        return switch (value) {
            case "toy" -> "개인/토이 프로젝트";
            case "production" -> "실제 운영 서비스";
            case "critical" -> "장애에 민감한 서비스";
            default -> "MVP/초기 서비스";
        };
    }

    private String labelServiceType(String value) {
        return switch (value) {
            case "static_site" -> "정적 웹사이트";
            case "web_api" -> "웹/API 서비스";
            case "mobile_app" -> "모바일 앱 백엔드";
            case "batch_worker" -> "배치/워커 서비스";
            case "internal_admin" -> "사내 관리자 도구";
            default -> "일반 서비스";
        };
    }
}
