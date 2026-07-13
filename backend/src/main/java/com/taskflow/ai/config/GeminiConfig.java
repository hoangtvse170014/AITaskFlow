package com.taskflow.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "gemini")
@Data
public class GeminiConfig {

    private String apiKey;

    private String model = "gemini-2.0-flash";

    private Integer maxTokens = 8192;

    private Double temperature = 0.7;

    private Integer timeoutSeconds = 120;

    private Integer maxRetries = 3;

    public boolean isValid() {
        return apiKey != null
                && !apiKey.isBlank()
                && !apiKey.startsWith("${")
                && !apiKey.contains("placeholder");
    }
}
