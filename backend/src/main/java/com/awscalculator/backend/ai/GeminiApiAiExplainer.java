package com.awscalculator.backend.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@link AiExplainer} backed by a real Gemini API call (via {@link GeminiApiClient}) instead of
 * shelling out to a locally installed CLI — see {@link ClaudeApiAiExplainer} for the same
 * rationale (no CLI binary in the deployed image). Select it with
 * ai.explainer.provider=gemini-api (see application.properties).
 */
@Component
public class GeminiApiAiExplainer implements AiExplainer {

    private final GeminiApiClient geminiApiClient;

    @Value("${ai.explainer.gemini.max-tokens:600}")
    private int maxTokens;

    public GeminiApiAiExplainer(GeminiApiClient geminiApiClient) {
        this.geminiApiClient = geminiApiClient;
    }

    @Override
    public String name() {
        return "gemini-api";
    }

    @Override
    public String explain(ExplanationContext context) {
        String prompt = ExplanationPromptBuilder.build(context);
        return geminiApiClient.complete(prompt, maxTokens);
    }
}
