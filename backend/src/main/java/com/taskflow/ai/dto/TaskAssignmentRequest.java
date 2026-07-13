package com.taskflow.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskAssignmentRequest {

    @NotBlank(message = "Task title is required")
    private String title;

    private String description;

    @NotNull(message = "Priority is required")
    private String priority;

    private LocalDate dueDate;

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotNull(message = "Workspace ID is required")
    private UUID workspaceId;
}
