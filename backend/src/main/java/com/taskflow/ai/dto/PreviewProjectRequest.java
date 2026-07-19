package com.taskflow.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Input for the read-only "Preview Project" workflow. Combines AI project planning
 * and AI task assignment into one reviewable response. Nothing is persisted.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewProjectRequest {

    @NotBlank(message = "Project idea is required")
    @Size(min = 10, max = 2000, message = "Project idea must be between 10 and 2000 characters")
    private String projectIdea;

    private String teamSize;

    private String technologyStack;

    private Integer weeksDeadline;

    @NotNull(message = "Workspace ID is required (used to resolve assignees)")
    private UUID workspaceId;

    /**
     * Optional human-readable reference name (not persisted) so the user can
     * recognize the preview in their history before approving.
     */
    private String referenceName;
}