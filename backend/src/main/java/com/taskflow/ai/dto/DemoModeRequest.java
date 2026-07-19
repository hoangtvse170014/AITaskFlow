package com.taskflow.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Single-shot input for Demo Mode. The user types a project idea (free-form)
 * and the backend drives the entire end-to-end pipeline:
 *
 * <ol>
 *   <li>Analyze requirements (AI Project Planner)</li>
 *   <li>Create project</li>
 *   <li>Create 4 sprints</li>
 *   <li>Create 35-50 tasks</li>
 *   <li>Create 100+ subtasks</li>
 *   <li>Assign tasks</li>
 *   <li>Calculate workload</li>
 *   <li>Generate risks</li>
 *   <li>Create timeline</li>
 *   <li>Refresh dashboard</li>
 * </ol>
 *
 * Nothing is persisted in the user's name until the autonomous creation
 * step at the end; everything before that is read-only planning.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DemoModeRequest {

    @NotBlank(message = "Project idea is required")
    @Size(min = 5, max = 2000, message = "Project idea must be between 5 and 2000 characters")
    private String projectIdea;

    @NotNull(message = "workspaceId is required")
    private UUID workspaceId;

    /** Optional technology hint - improves AI planner output quality. */
    private String technologyStack;

    /** Optional team size hint (e.g. "5", "team of 8 devs"). */
    private String teamSize;

    /** Optional deadline in weeks - falls back to 8 weeks if omitted. */
    private Integer weeksDeadline;

    /**
     * Optional idempotency key. If the same key is used twice the second
     * call returns the existing project without creating duplicates.
     */
    private String idempotencyKey;
}