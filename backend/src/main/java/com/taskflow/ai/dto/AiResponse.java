package com.taskflow.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiResponse {

    private boolean success;

    private String response;

    private String model;

    private Long processingTimeMs;

    private Instant timestamp;

    private String error;

    public static AiResponse success(String response, String model, long processingTimeMs) {
        return AiResponse.builder()
                .success(true)
                .response(response)
                .model(model)
                .processingTimeMs(processingTimeMs)
                .timestamp(Instant.now())
                .build();
    }

    public static AiResponse error(String error, String model) {
        return AiResponse.builder()
                .success(false)
                .error(error)
                .model(model)
                .timestamp(Instant.now())
                .build();
    }
}
