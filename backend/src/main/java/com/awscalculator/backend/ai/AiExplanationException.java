package com.awscalculator.backend.ai;

public class AiExplanationException extends RuntimeException {
    public AiExplanationException(String message) {
        super(message);
    }

    public AiExplanationException(String message, Throwable cause) {
        super(message, cause);
    }
}
