package com.taskflow.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchTaskAssignmentRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotNull(message = "Workspace ID is required")
    private UUID workspaceId;

    @NotEmpty(message = "At least one task is required")
    @Valid
    private List<TaskInput> tasks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TaskInput {
        @NotNull(message = "Task reference is required (id or title)")
        private String taskRef;

        private String title;
        private String description;
        private String priority;
        private LocalDate dueDate;

        /**
         * Optional category hint to help AI routing: BACKEND, FRONTEND, UI, DOCUMENT, FULLSTACK, OTHER.
         * If null, AI infers from title/description.
         */
        private String category;
    }
}