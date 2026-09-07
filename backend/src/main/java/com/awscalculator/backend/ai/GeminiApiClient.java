package com.awscalculator.backend.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Thin client for the Google Gemini "generateContent" API, shared by {@link GeminiApiAiExplainer}
 * and the recommendation package's GeminiApiArchitectureRecommendationAdvisor. Configured via
 * ai.gemini.* / GEMINI_* — see application.properties. A free API key (no credit card, generous
 * free-tier rate limits) is available at https://aistudio.google.com/apikey. Mirrors the plain
 * HttpClient style already used for external calls elsewhere in this codebase (XTokenClient,
 * ClaudeApiClient) rather than introducing a new HTTP client abstraction.
 */
@Component
public class GeminiApiClient {

    private static final String API_VERSION = "v1beta";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;

    @Value("${ai.gemini.api-key:}")
    private String apiKey;

    @Value("${ai.gemini.model:gemini-3.5-flash-lite}")
    private String model;

    @Value("${ai.gemini.timeout-seconds:60}")
    private long timeoutSeconds;

    public GeminiApiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String complete(String userPrompt, int maxOutputTokens) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiExplanationException("Gemini API key is not configured (GEMINI_API_KEY)");
        }

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", userPrompt))
                )),
                "generationConfig", Map.of("maxOutputTokens", maxOutputTokens)
        );

        String url = "https://generativelanguage.googleapis.com/" + API_VERSION
                + "/models/" + model + ":generateContent";

        try {
            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("x-goog-api-key", apiKey)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiExplanationException("Gemini API HTTP " + response.statusCode() + ": " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new AiExplanationException("Gemini API response had no candidates");
            }
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                throw new AiExplanationException("Gemini API candidate had no content parts");
            }
            StringBuilder text = new StringBuilder();
            for (JsonNode part : parts) {
                text.append(part.path("text").asString(""));
            }
            if (text.isEmpty()) {
                throw new AiExplanationException("Gemini API returned empty text");
            }
            return text.toString().trim();
        } catch (AiExplanationException e) {
            throw e;
        } catch (Exception e) {
            throw new AiExplanationException("Gemini API call failed: " + e.getMessage(), e);
        }
    }
}
