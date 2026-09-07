package com.awscalculator.backend.recommendation;

import com.awscalculator.backend.recommendation.ArchitectureTier.TierResourceSpec;
import com.awscalculator.backend.resource.ResourceType;
import java.util.List;
import java.util.Map;

/**
 * Fixed preset architectures used only by {@link RuleBasedArchitectureRecommendationAdvisor}, the
 * deterministic fallback for when no AI advisor is configured or the primary one fails. The
 * primary path ({@code claude-api}/{@code gemini-api}/{@code codex-cli}) composes an architecture
 * freely from the resource catalog instead of picking one of these — see
 * {@link ArchitectureRecommendationPromptBuilder} and {@link ArchitectureRecommendationSanitizer}.
 */
public final class ArchitectureTiers {

    public static final List<ArchitectureTier> TIERS = List.of(
            new ArchitectureTier(
                    "미니멀 (프로토타입)",
                    "혼자 써보거나 프로토타입을 배포하는 수준의 최소 구성",
                    List.of(
                            new TierResourceSpec(ResourceType.ECS,
                                    Map.of("cpu", 0.25, "memory", 0.5, "tasks", 1, "architecture", "arm64")),
                            new TierResourceSpec(ResourceType.S3, Map.of("storageGb", 5))
                    )
            ),
            new ArchitectureTier(
                    "스타트업",
                    "초기 서비스 오픈 수준 — ECS 2대 + RDS + ALB",
                    List.of(
                            new TierResourceSpec(ResourceType.ECS,
                                    Map.of("cpu", 0.5, "memory", 1, "tasks", 2, "architecture", "arm64")),
                            new TierResourceSpec(ResourceType.RDS,
                                    Map.of("instanceType", "db.t4g.micro", "storageGb", 20)),
                            new TierResourceSpec(ResourceType.ALB, Map.of("enabled", true)),
                            new TierResourceSpec(ResourceType.S3, Map.of("storageGb", 10))
                    )
            ),
            new ArchitectureTier(
                    "성장기",
                    "트래픽이 붙기 시작한 서비스 — Redis 캐시 + CloudFront 추가",
                    List.of(
                            new TierResourceSpec(ResourceType.ECS,
                                    Map.of("cpu", 1, "memory", 2, "tasks", 2, "architecture", "arm64")),
                            new TierResourceSpec(ResourceType.RDS,
                                    Map.of("instanceType", "db.t4g.small", "storageGb", 30)),
                            new TierResourceSpec(ResourceType.REDIS,
                                    Map.of("nodeType", "cache.t4g.micro", "nodes", 1)),
                            new TierResourceSpec(ResourceType.ALB, Map.of("enabled", true)),
                            new TierResourceSpec(ResourceType.S3, Map.of("storageGb", 50)),
                            new TierResourceSpec(ResourceType.CLOUDFRONT, Map.of("dataTransferGb", 50))
                    )
            ),
            new ArchitectureTier(
                    "트래픽 대응형",
                    "본격적인 프로덕션 트래픽을 받는 서비스 — 이중화 + NAT Gateway 포함",
                    List.of(
                            new TierResourceSpec(ResourceType.ECS,
                                    Map.of("cpu", 2, "memory", 4, "tasks", 3, "architecture", "arm64")),
                            new TierResourceSpec(ResourceType.RDS,
                                    Map.of("instanceType", "db.r6g.large", "storageGb", 100)),
                            new TierResourceSpec(ResourceType.REDIS,
                                    Map.of("nodeType", "cache.t4g.small", "nodes", 2)),
                            new TierResourceSpec(ResourceType.ALB, Map.of("enabled", true)),
                            new TierResourceSpec(ResourceType.S3, Map.of("storageGb", 100)),
                            new TierResourceSpec(ResourceType.CLOUDFRONT, Map.of("dataTransferGb", 200)),
                            new TierResourceSpec(ResourceType.NAT_GATEWAY, Map.of("gatewayCount", 1, "processedGb", 100))
                    )
            )
    );

    private ArchitectureTiers() {
    }
}
