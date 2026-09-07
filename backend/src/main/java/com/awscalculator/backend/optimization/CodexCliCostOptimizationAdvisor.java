package com.awscalculator.backend.optimization;

import com.awscalculator.backend.ai.AiExplanationException;
import com.awscalculator.backend.optimization.dto.Suggestion;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * {@link CostOptimizationAdvisor} backed by a locally installed `codex` CLI — local dev only,
 * there's no `codex` binary in the deployed image. Select it with
 * ai.optimization.provider=codex-cli (the default; see application.properties).
 */
@Component
public class CodexCliCostOptimizationAdvisor implements CostOptimizationAdvisor {

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String codexCommand;
    private final long timeoutSeconds;

    public CodexCliCostOptimizationAdvisor(
            ObjectMapper objectMapper,
            @Value("${ai.optimization.enabled:${ai.explainer.enabled:true}}") boolean enabled,
            @Value("${ai.optimization.codex-command:${ai.explainer.codex-command:codex}}") String codexCommand,
            @Value("${ai.optimization.timeout-seconds:120}") long timeoutSeconds
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
    public CostOptimizationAdvice advise(CostOptimizationContext context) {
        if (!enabled) {
            throw new AiExplanationException("codex-cli optimization advisor is disabled");
        }

        String prompt = CostOptimizationPromptBuilder.build(context);
        String output = runCodex(prompt);
        try {
            AdvicePayload payload = objectMapper.readValue(jsonObjectFrom(output), AdvicePayload.class);
            List<Suggestion> suggestions = payload.suggestions() == null ? List.of() : payload.suggestions();
            return new CostOptimizationAdvice(suggestions, name(), true);
        } catch (JacksonException e) {
            throw new AiExplanationException("failed to parse codex optimization JSON: " + e.getMessage(), e);
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

    private String runCodex(String prompt) {
        Path scratchDir = null;
        Path outputFile = null;
        try {
            scratchDir = Files.createTempDirectory("aws-calc-optimization-");
            outputFile = Files.createTempFile(scratchDir, "optimization-", ".json");

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

    private record AdvicePayload(List<Suggestion> suggestions) {
    }
}
