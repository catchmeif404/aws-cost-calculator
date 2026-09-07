package com.awscalculator.backend.recommendation;

import com.awscalculator.backend.ai.AiExplanationException;
import com.awscalculator.backend.ai.GeminiApiClient;
import com.awscalculator.backend.recommendation.dto.AiResourceSpec;
import com.awscalculator.backend.recommendation.dto.ServiceRecommendationRequest;
import com.awscalculator.backend.resource.catalog.ResourceCatalogItem;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * {@link ArchitectureRecommendationAdvisor} backed by a real Gemini API call instead of shelling
 * out to a locally installed `codex` CLI — see
 * {@link com.awscalculator.backend.ai.GeminiApiAiExplainer} for why. Select it with
 * ai.recommendation.provider=gemini-api (see application.properties).
 */
@Component
public class GeminiApiArchitectureRecommendationAdvisor implements ArchitectureRecommendationAdvisor {

    private final GeminiApiClient geminiApiClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.recommendation.gemini.max-tokens:1600}")
    private int maxTokens;

    public GeminiApiArchitectureRecommendationAdvisor(GeminiApiClient geminiApiClient, ObjectMapper objectMapper) {
        this.geminiApiClient = geminiApiClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "gemini-api";
    }

    @Override
    public ArchitectureRecommendationAdvice advise(
            ServiceRecommendationRequest request,
            long estimatedMonthlyRequests,
            List<ResourceCatalogItem> catalog,
            String locale
    ) {
        String prompt = ArchitectureRecommendationPromptBuilder.build(request, estimatedMonthlyRequests, catalog, locale);
        String output = geminiApiClient.complete(prompt, maxTokens);
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
            throw new AiExplanationException("failed to parse gemini-api recommendation JSON: " + e.getMessage(), e);
        }
    }

    private String jsonObjectFrom(String output) {
        int start = output.indexOf('{');
        int end = output.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new AiExplanationException("gemini-api produced no JSON object");
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
