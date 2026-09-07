package com.awscalculator.backend.optimization;

import com.awscalculator.backend.ai.AiExplanationException;
import com.awscalculator.backend.ai.ClaudeApiClient;
import com.awscalculator.backend.optimization.dto.Suggestion;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * {@link CostOptimizationAdvisor} backed by a real Anthropic API call. Select it with
 * ai.optimization.provider=claude-api (see application.properties).
 */
@Component
public class ClaudeApiCostOptimizationAdvisor implements CostOptimizationAdvisor {

    private final ClaudeApiClient claudeApiClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.optimization.claude.max-tokens:900}")
    private int maxTokens;

    public ClaudeApiCostOptimizationAdvisor(ClaudeApiClient claudeApiClient, ObjectMapper objectMapper) {
        this.claudeApiClient = claudeApiClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "claude-api";
    }

    @Override
    public CostOptimizationAdvice advise(CostOptimizationContext context) {
        String prompt = CostOptimizationPromptBuilder.build(context);
        String output = claudeApiClient.complete(prompt, maxTokens);
        try {
            AdvicePayload payload = objectMapper.readValue(jsonObjectFrom(output), AdvicePayload.class);
            List<Suggestion> suggestions = payload.suggestions() == null ? List.of() : payload.suggestions();
            return new CostOptimizationAdvice(suggestions, name(), true);
        } catch (JacksonException e) {
            throw new AiExplanationException("failed to parse claude-api optimization JSON: " + e.getMessage(), e);
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

    private record AdvicePayload(List<Suggestion> suggestions) {
    }
}
