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
public class WorkspaceAnswerResponse {

    private String answer;

    private List<String> sources;

    private Double confidence;

    private List<RelatedItem> relatedProjects;

    private List<RelatedItem> relatedTasks;

    private List<RelatedItem> relatedMembers;

    private List<RelatedItem> relatedGoals;

    private List<RelatedItem> relatedPages;

    private List<String> suggestions;

    /** Detected intent for the question, e.g. SUMMARIZE_WORKSPACE or MOST_RISKY_PROJECT. */
    private String intent;

    private Long processingTimeMs;

    private String error;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RelatedItem {
        private String id;
        private String name;
        private String type;
        private String status;
        private String description;
    }

    public static WorkspaceAnswerResponse success(
            String answer,
            List<String> sources,
            Double confidence,
            List<RelatedItem> relatedProjects,
            List<RelatedItem> relatedTasks,
            List<String> suggestions,
            Long processingTimeMs) {
        return WorkspaceAnswerResponse.builder()
                .answer(answer)
                .sources(sources)
                .confidence(confidence)
                .relatedProjects(relatedProjects)
                .relatedTasks(relatedTasks)
                .suggestions(suggestions)
                .processingTimeMs(processingTimeMs)
                .build();
    }

    public static WorkspaceAnswerResponse error(String error) {
        return WorkspaceAnswerResponse.builder()
                .error(error)
                .build();
    }
}
