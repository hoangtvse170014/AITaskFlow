package com.taskflow.ai.exception;

public class AiTimeoutException extends AiException {

    private final int timeoutSeconds;

    public AiTimeoutException(String message, int timeoutSeconds) {
        super(message);
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }
}
