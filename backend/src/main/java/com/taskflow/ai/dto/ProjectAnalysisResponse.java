package com.taskflow.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectAnalysisResponse {

    private Integer healthScore;

    private String summary;

    private List<String> risks;

    private List<String> recommendations;

    private List<String> nextActions;

    private Double confidence;

    private Long processingTimeMs;

    private String error;

    public static ProjectAnalysisResponse success(
            Integer healthScore,
            String summary,
            List<String> risks,
            List<String> recommendations,
            List<String> nextActions,
            Double confidence,
            Long processingTimeMs) {
        return ProjectAnalysisResponse.builder()
                .healthScore(healthScore)
                .summary(summary)
                .risks(risks)
                .recommendations(recommendations)
                .nextActions(nextActions)
                .confidence(confidence)
                .processingTimeMs(processingTimeMs)
                .build();
    }

    public static ProjectAnalysisResponse error(String error) {
        return ProjectAnalysisResponse.builder()
                .error(error)
                .build();
    }
}
