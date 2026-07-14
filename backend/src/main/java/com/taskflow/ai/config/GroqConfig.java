package com.taskflow.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "groq")
@Data
public class GroqConfig {

    private String apiKey;

    private String model = "llama-3.3-70b-versatile";

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
