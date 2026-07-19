package com.taskflow.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SprintGenerateRequest {

    @NotNull(message = "Workspace ID is required")
    private UUID workspaceId;

    /**
     * Optional: pass a previously answered sprint context snapshot to continue.
     * If null, a fresh sprint is generated from scratch.
     */
    private UUID basedOnProjectId;

    /** Sprint duration in days. Defaults to 14 (2 weeks). */
    @Builder.Default
    private Integer durationDays = 14;

    /** Target number of tasks to include. Defaults to 10. */
    @Builder.Default
    private Integer maxTasks = 10;

    /** Include DONE tasks in the plan (for carry-over). Defaults to false. */
    @Builder.Default
    private Boolean includeDone = false;
}
