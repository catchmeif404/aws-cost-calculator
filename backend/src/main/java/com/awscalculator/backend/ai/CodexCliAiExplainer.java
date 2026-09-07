package com.awscalculator.backend.ai;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Primary {@link AiExplainer} for MVP: shells out to the locally installed `codex` CLI
 * (`codex exec`) instead of calling an LLM API directly. This is a stand-in adapter — swap it
 * for a ClaudeApiAiExplainer/OpenAiApiAiExplainer later without touching {@link ExplanationService}.
 *
 * Known limitation: `codex exec` boots a full coding-agent session per call, so a single
 * explanation can take several seconds to tens of seconds. Fine for an on-demand "AI 설명"
 * button; not something to put in the hot path of the numeric calculate/optimize endpoints.
 */
@Component
public class CodexCliAiExplainer implements AiExplainer {

    @Value("${ai.explainer.enabled:true}")
    private boolean enabled;

    @Value("${ai.explainer.codex-command:codex}")
    private String codexCommand;

    @Value("${ai.explainer.timeout-seconds:45}")
    private long timeoutSeconds;

    @Override
    public String name() {
        return "codex-cli";
    }

    @Override
    public String explain(ExplanationContext context) {
        if (!enabled) {
            throw new AiExplanationException("codex-cli explainer is disabled (ai.explainer.enabled=false)");
        }

        String prompt = ExplanationPromptBuilder.build(context);
        Path scratchDir = null;
        Path outputFile = null;
        try {
            scratchDir = Files.createTempDirectory("aws-calc-codex-");
            outputFile = Files.createTempFile(scratchDir, "explain-", ".txt");

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
            // best-effort cleanup of a scratch file/dir
        }
    }
}
