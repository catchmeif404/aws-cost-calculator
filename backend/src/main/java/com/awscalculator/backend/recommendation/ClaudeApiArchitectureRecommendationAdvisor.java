package com.awscalculator.backend.recommendation;

import com.awscalculator.backend.ai.AiExplanationException;
import com.awscalculator.backend.ai.ClaudeApiClient;
import com.awscalculator.backend.recommendation.dto.AiResourceSpec;
import com.awscalculator.backend.recommendation.dto.ServiceRecommendationRequest;
import com.awscalculator.backend.resource.catalog.ResourceCatalogItem;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * {@link ArchitectureRecommendationAdvisor} backed by a real Anthropic API call instead of
 * shelling out to a locally installed `codex` CLI — see
 * {@link com.awscalculator.backend.ai.ClaudeApiAiExplainer} for why. Select it with
 * ai.recommendation.provider=claude-api (see application.properties).
 */
@Component
public class ClaudeApiArchitectureRecommendationAdvisor implements ArchitectureRecommendationAdvisor {

    private final ClaudeApiClient claudeApiClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.recommendation.claude.max-tokens:1600}")
    private int maxTokens;

    public ClaudeApiArchitectureRecommendationAdvisor(ClaudeApiClient claudeApiClient, ObjectMapper objectMapper) {
        this.claudeApiClient = claudeApiClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "claude-api";
    }

    @Override
    public ArchitectureRecommendationAdvice advise(
            ServiceRecommendationRequest request,
            long estimatedMonthlyRequests,
            List<ResourceCatalogItem> catalog,
            String locale
    ) {
        String prompt = ArchitectureRecommendationPromptBuilder.build(request, estimatedMonthlyRequests, catalog, locale);
        String output = claudeApiClient.complete(prompt, maxTokens);
        try {
            AdvicePayload payload = objectMapper.readValue(jsonObjectFrom(output), AdvicePayload.class);
            return new ArchitectureRecommendationAdvice(
                    payload.architectureName(),
                    payload.architectureDescription(),
                    payload.resources() == null ? List.of() : payload.resources(),
                    cleanTerms(payload.recommendationReason()),
                    payload.additionalRecommendations() == null
                            ? List.of()
                            : payload.additionalRecommendations().stream().map(this::cleanTerms).toList(),
                    name(),
                    true
            );
        } catch (JacksonException e) {
            throw new AiExplanationException("failed to parse claude-api recommendation JSON: " + e.getMessage(), e);
        }
    }

    private String jsonObjectFrom(String output) {
        int start = output.indexOf('{');
        int end = output.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new AiExplanationException("claude-api produced no JSON object");
        }
        return output.substring(start, end + 1);
    }

    private String cleanTerms(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("피크", "가장 바쁜 시간대");
    }

    private record AdvicePayload(
            String architectureName,
            String architectureDescription,
            List<AiResourceSpec> resources,
            String recommendationReason,
            List<String> additionalRecommendations
    ) {
    }
}
