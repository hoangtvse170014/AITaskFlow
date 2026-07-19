package com.taskflow.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response payload for Demo Mode. The backend runs the full pipeline in a
 * single call and returns the human-friendly summary plus every stage's
 * status. The frontend renders the six progress steps as an animated
 * timeline.
 *
 * <p>The {@code stages} list is ordered to mirror the visual progress bar:
 * <ol>
 *   <li>ANALYZING_REQUIREMENTS</li>
 *   <li>PLANNING_SPRINTS</li>
 *   <li>GENERATING_TASKS</li>
 *   <li>ASSIGNING_MEMBERS</li>
 *   <li>CREATING_ENTITIES</li>
 *   <li>REFRESHING_DASHBOARD</li>
 * </ol>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DemoModeResponse {

    /** True when the entire pipeline succeeded. */
    private boolean success;

    /** True when this call hit the idempotency cache and returned the existing project. */
    private boolean idempotent;

    /** Stage that is currently in progress or null when done. */
    private DemoStage currentStage;

    /** Ordered list of stages with per-stage status (PENDING, RUNNING, DONE, FAILED, SKIPPED). */
    private List<DemoStageResult> stages;

    /** Counters at the end of the run. */
    private Counts counts;

    /** Generated timeline (start/end/estimated weeks). */
    private TimelineInfo timeline;

    /** Workload summary - how many tasks per member. */
    private List<WorkloadEntry> workload;

    /** Top risks aggregated from the AI planner. */
    private List<String> risks;

    /** The newly created (or existing) project id. */
    private UUID projectId;

    /** Project name returned by the AI planner. */
    private String projectName;

    /** Refresh signals the frontend should react to. */
    private List<String> refreshSignals;

    /** Log of every micro-step the AI took. */
    private List<String> steps;

    /** Warnings that did not abort the run. */
    private List<String> warnings;

    /** When the run started and finished. */
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long processingTimeMs;

    /** Error envelope when the pipeline failed before completion. */
    private String error;

    public static DemoModeResponse fail(String message, DemoStage failedAt) {
        return DemoModeResponse.builder()
                .success(false)
                .currentStage(failedAt)
                .error(message)
                .startedAt(LocalDateTime.now())
                .finishedAt(LocalDateTime.now())
                .build();
    }

    public enum DemoStage {
        ANALYZING_REQUIREMENTS,
        PLANNING_SPRINTS,
        GENERATING_TASKS,
        ASSIGNING_MEMBERS,
        CREATING_ENTITIES,
        REFRESHING_DASHBOARD,
        COMPLETED
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DemoStageResult {
        private DemoStage stage;
        private String label;
        private StageStatus status;
        private Long durationMs;
        private String detail;
    }

    public enum StageStatus {
        PENDING,
        RUNNING,
        DONE,
        FAILED,
        SKIPPED
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Counts {
        private int sprintsCreated;
        private int tasksCreated;
        private int subtasksCreated;
        private int assignmentsApplied;
        private int risksGenerated;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimelineInfo {
        private String startDate;
        private String endDate;
        private Integer totalWeeks;
        private Integer totalEstimatedHours;
        private Integer totalStoryPoints;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WorkloadEntry {
        private String memberId;
        private String memberName;
        private Integer taskCount;
        private Integer estimatedHours;
        private Double workloadPercentage;
    }
}