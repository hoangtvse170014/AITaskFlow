package com.taskflow.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Result of an autonomous project creation run. The frontend can use
 * {@code refreshSignals} to invalidate local caches/dashboards.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateProjectFromPreviewResponse {

    private boolean success;
    private boolean idempotent;          // true when nothing was created because the project already exists
    private UUID projectId;
    private String projectName;
    private Integer sprintsCreated;      // encoded as Task labels
    private Integer tasksCreated;
    private Integer subtasksCreated;
    private Integer assignmentsApplied;
    private List<String> steps;           // ordered log of each step (project, sprint, task, subtask, assignment)
    private List<String> warnings;
    private Long processingTimeMs;
    private String error;

    /** Resource types that the frontend should refetch. */
    private List<String> refreshSignals;

    public static CreateProjectFromPreviewResponse fail(String message) {
        return CreateProjectFromPreviewResponse.builder()
                .success(false)
                .error(message)
                .refreshSignals(List.of())
                .build();
    }
}