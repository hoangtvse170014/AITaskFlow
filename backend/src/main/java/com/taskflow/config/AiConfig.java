package com.taskflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai")
@Data
public class AiConfig {

    private String provider = "openai";
    private String model = "gpt-4o-mini";
    private String apiKey;
    private Double temperature = 0.7;
    private Integer maxTokens = 1000;
    private Integer timeoutSeconds = 30;
    private Integer retryAttempts = 3;
    private Boolean enabled = true;
}
