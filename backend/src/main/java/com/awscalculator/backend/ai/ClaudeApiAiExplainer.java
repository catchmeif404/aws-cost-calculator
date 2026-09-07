package com.awscalculator.backend.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@link AiExplainer} backed by a real Anthropic API call (via {@link ClaudeApiClient}) instead
 * of shelling out to a locally installed CLI — the piece {@link CodexCliAiExplainer}'s own
 * docstring called out as the eventual swap-in, and the one that actually works in a deployed
 * environment (Railway) where no `codex` binary is installed. Select it with
 * ai.explainer.provider=claude-api (see application.properties).
 */
@Component
public class ClaudeApiAiExplainer implements AiExplainer {

    private final ClaudeApiClient claudeApiClient;

    @Value("${ai.explainer.claude.max-tokens:600}")
    private int maxTokens;

    public ClaudeApiAiExplainer(ClaudeApiClient claudeApiClient) {
        this.claudeApiClient = claudeApiClient;
    }

    @Override
    public String name() {
        return "claude-api";
    }

    @Override
    public String explain(ExplanationContext context) {
        String prompt = ExplanationPromptBuilder.build(context);
        return claudeApiClient.complete(prompt, maxTokens);
    }
}
