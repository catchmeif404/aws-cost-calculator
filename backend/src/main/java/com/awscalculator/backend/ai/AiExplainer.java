package com.awscalculator.backend.ai;

/**
 * Adapter interface for turning an already-computed {@link ExplanationContext} into a natural
 * language explanation. Swap providers (codex-cli, Claude API, OpenAI API, ...) by adding a new
 * implementation and pointing {@link ExplanationService} at it — the rest of the app never changes.
 */
public interface AiExplainer {

    /** Short identifier returned to the client so it can show which provider actually answered. */
    String name();

    /**
     * @throws AiExplanationException if this provider could not produce an explanation
     *         (not configured, unreachable, timed out, ...). Callers should fall back.
     */
    String explain(ExplanationContext context);
}
