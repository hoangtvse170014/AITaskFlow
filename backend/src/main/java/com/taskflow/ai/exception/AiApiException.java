package com.taskflow.ai.exception;

public class AiApiException extends AiException {

    private final int statusCode;
    private final String errorType;

    public AiApiException(String message, int statusCode, String errorType) {
        super(message);
        this.statusCode = statusCode;
        this.errorType = errorType;
    }

    public AiApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorType = "API_ERROR";
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorType() {
        return errorType;
    }

    public boolean isRateLimitError() {
        return statusCode == 429;
    }

    public boolean isAuthError() {
        return statusCode == 401 || statusCode == 403;
    }

    public boolean isServerError() {
        return statusCode >= 500;
    }
}
