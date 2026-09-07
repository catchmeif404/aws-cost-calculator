package com.awscalculator.backend.recommendation;

import com.awscalculator.backend.ai.AiExplanationException;
import com.awscalculator.backend.recommendation.dto.AiResourceSpec;
import com.awscalculator.backend.recommendation.dto.ServiceRecommendationRequest;
import com.awscalculator.backend.resource.catalog.ResourceCatalogItem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class CodexCliArchitectureRecommendationAdvisor implements ArchitectureRecommendationAdvisor {

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String codexCommand;
    private final long timeoutSeconds;

    public CodexCliArchitectureRecommendationAdvisor(
            ObjectMapper objectMapper,
            @Value("${ai.recommendation.enabled:${ai.explainer.enabled:true}}") boolean enabled,
            @Value("${ai.recommendation.codex-command:${ai.explainer.codex-command:codex}}") String codexCommand,
            @Value("${ai.recommendation.timeout-seconds:120}") long timeoutSeconds
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.codexCommand = codexCommand;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String name() {
        return "codex-cli";
    }

    @Override
    public ArchitectureRecommendationAdvice advise(
            ServiceRecommendationRequest request,
            long estimatedMonthlyRequests,
            List<ResourceCatalogItem> catalog,
            String locale
    ) {
        if (!enabled) {
            throw new AiExplanationException("codex-cli recommendation advisor is disabled");
        }

        String prompt = ArchitectureRecommendationPromptBuilder.build(request, estimatedMonthlyRequests, catalog, locale);
        String output = runCodex(prompt);
        try {
            CodexAdvicePayload payload = objectMapper.readValue(jsonObjectFrom(output), CodexAdvicePayload.class);
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
            throw new AiExplanationException("failed to parse codex recommendation JSON: " + e.getMessage(), e);
        }
    }

    private String jsonObjectFrom(String output) {
        int start = output.indexOf('{');
        int end = output.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new AiExplanationException("codex exec produced no JSON object");
        }
        return output.substring(start, end + 1);
    }

    private String cleanTerms(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("피크", "가장 바쁜 시간대");
    }

    private String runCodex(String prompt) {
        Path scratchDir = null;
        Path outputFile = null;
        try {
            scratchDir = Files.createTempDirectory("aws-calc-recommendation-");
            outputFile = Files.createTempFile(scratchDir, "recommendation-", ".json");

            ProcessBuilder processBuilder = new ProcessBuilder(
                    codexCommand, "exec",
                    "--skip-git-repo-check",
                    "--sandbox", "read-only",
                    "--ephemeral",
                    "-o", outputFile.toString(),
                    prompt
            );
            processBuilder.directory(scratchDir.toFile());
            processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);

            Process process = processBuilder.start();
            process.getOutputStream().close();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new AiExplanationException("codex exec timed out after " + timeoutSeconds + "s");
            }
            if (process.exitValue() != 0) {
                throw new AiExplanationException("codex exec exited with code " + process.exitValue());
            }

            String text = Files.readString(outputFile).trim();
            if (text.isEmpty()) {
                throw new AiExplanationException("codex exec produced no output");
            }
            return text;
        } catch (IOException e) {
            throw new AiExplanationException("failed to run codex exec: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiExplanationException("codex exec was interrupted", e);
        } finally {
            deleteQuietly(outputFile);
            deleteQuietly(scratchDir);
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 임시 파일 정리는 실패해도 추천 응답에는 영향을 주지 않습니다.
        }
    }

    private record CodexAdvicePayload(
            String architectureName,
            String architectureDescription,
            List<AiResourceSpec> resources,
            String recommendationReason,
            List<String> additionalRecommendations
    ) {
    }
}
