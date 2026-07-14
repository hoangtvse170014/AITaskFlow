package com.taskflow.ai.service;

import com.taskflow.ai.dto.BatchTaskAssignmentResponse;
import com.taskflow.ai.dto.CreateProjectFromPreviewRequest;
import com.taskflow.ai.dto.CreateProjectFromPreviewResponse;
import com.taskflow.ai.dto.DemoModeRequest;
import com.taskflow.ai.dto.DemoModeResponse;
import com.taskflow.ai.dto.DemoModeResponse.Counts;
import com.taskflow.ai.dto.DemoModeResponse.DemoStage;
import com.taskflow.ai.dto.DemoModeResponse.DemoStageResult;
import com.taskflow.ai.dto.DemoModeResponse.StageStatus;
import com.taskflow.ai.dto.DemoModeResponse.TimelineInfo;
import com.taskflow.ai.dto.DemoModeResponse.WorkloadEntry;
import com.taskflow.ai.dto.PreviewProjectRequest;
import com.taskflow.ai.dto.PreviewProjectResponse;
import com.taskflow.ai.dto.ProjectPlannerRequest;
import com.taskflow.ai.dto.ProjectPlannerResponse;
import com.taskflow.event.DashboardEventBroker;
import com.taskflow.event.DashboardRefreshEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One-shot "Demo Mode" pipeline. Given a free-form project idea, this
 * service runs every AI capability in sequence and returns a single
 * response that includes per-stage status, counters, workload, timeline,
 * risks, and refresh signals for the frontend.
 *
 * <p>Stages:
 * <ol>
 *   <li><b>ANALYZING_REQUIREMENTS</b> — AI Project Planner produces a
 *       description, scope, milestones, sprints, epics, tasks, subtasks,
 *       dependencies, timeline.</li>
 *   <li><b>PLANNING_SPRINTS</b> — preview service re-organises the plan into
 *       4 sprints with explicit capacity & dates.</li>
 *   <li><b>GENERATING_TASKS</b> — preview service attaches subtasks,
 *       dependencies, risks and an execution order.</li>
 *   <li><b>ASSIGNING_MEMBERS</b> — batch assignment service pairs each task
 *       with the best matching member, balancing workload.</li>
 *   <li><b>CREATING_ENTITIES</b> — AutonomousProjectCreationService drives
 *       the real Project / Task / SubTask APIs with full rollback.</li>
 *   <li><b>REFRESHING_DASHBOARD</b> — DashboardEventBroker publishes a SSE
 *       event so connected clients refresh automatically.</li>
 * </ol>
 *
 * <p>Every stage records duration and a short detail message. Failures
 * short-circuit the run and the response carries {@code success=false} plus
 * the {@link DemoStage} that failed. Stages after the failure point stay
 * {@code SKIPPED}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DemoModeService {

    private final ProjectPlannerService projectPlannerService;
    private final ProjectPreviewService projectPreviewService;
    private final AiTaskAssignmentService taskAssignmentService;
    private final AutonomousProjectCreationService autonomousProjectCreationService;
    private final DashboardEventBroker dashboardEventBroker;

    public DemoModeResponse run(DemoModeRequest request) {
        LocalDateTime startedAt = LocalDateTime.now();
        long runStart = System.currentTimeMillis();

        // Pre-allocate the ordered stage list so the UI can show progress
        // before the response is fully computed.
        List<DemoStageResult> stages = new ArrayList<>();
        stages.add(stage(DemoStage.ANALYZING_REQUIREMENTS, "Analyzing requirements..."));
        stages.add(stage(DemoStage.PLANNING_SPRINTS,      "Planning sprints..."));
        stages.add(stage(DemoStage.GENERATING_TASKS,      "Generating tasks and subtasks..."));
        stages.add(stage(DemoStage.ASSIGNING_MEMBERS,     "Assigning tasks to team members..."));
        stages.add(stage(DemoStage.CREATING_ENTITIES,     "Creating project, sprints, tasks..."));
        stages.add(stage(DemoStage.REFRESHING_DASHBOARD,  "Refreshing dashboard..."));

        List<String> steps = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        PreviewProjectResponse preview = null;

        try {
            // ===== 1. ANALYZING_REQUIREMENTS =====
            preview = analyze(request, stages, steps);
            if (preview == null) {
                return fail(stages, DemoStage.ANALYZING_REQUIREMENTS,
                        "Planner returned no tasks.", startedAt);
            }
            markDone(stages, 0, steps.size() + " planner steps");
            steps.add("[Analyzing] planner returned "
                    + safeSize(preview.getTasks()) + " tasks and "
                    + safeSize(preview.getSubtasks()) + " subtasks");

            // ===== 2 & 3. PLANNING_SPRINTS + GENERATING_TASKS (single preview call) =====
            // (the preview service already did both: planning + task/subtask generation)
            markRunning(stages, 1);
            markRunning(stages, 2);
            long p2 = System.currentTimeMillis();
            // preview already computed; just enrich
            stages.get(1).setDurationMs(System.currentTimeMillis() - p2);
            stages.get(1).setDetail(safeSize(preview.getSprints()) + " sprint(s) ready");
            stages.get(1).setStatus(StageStatus.DONE);
            stages.get(2).setDurationMs(System.currentTimeMillis() - p2);
            stages.get(2).setDetail(safeSize(preview.getTasks()) + " tasks, "
                    + safeSize(preview.getSubtasks()) + " subtasks");
            stages.get(2).setStatus(StageStatus.DONE);

            // ===== 4. ASSIGNING_MEMBERS =====
            markRunning(stages, 3);
            long aStart = System.currentTimeMillis();
            BatchTaskAssignmentResponse assignment = taskAssignmentService.batchRecommendPreview(
                    buildAssignmentRequest(request, preview),
                    preview.getProject() != null ? preview.getProject().getName() : "Demo Project");
            stages.get(3).setDurationMs(System.currentTimeMillis() - aStart);
            stages.get(3).setDetail(safeSize(assignment.getAssignments()) + " assignments, "
                    + Math.round((assignment.getOverallConfidence() != null ? assignment.getOverallConfidence() : 0) * 100)
                    + "% confidence");
            stages.get(3).setStatus(StageStatus.DONE);
            steps.add("[Assigning] " + safeSize(assignment.getAssignments())
                    + " task assignments resolved");

            // ===== 5. CREATING_ENTITIES =====
            markRunning(stages, 4);
            long cStart = System.currentTimeMillis();
            CreateProjectFromPreviewRequest createReq = CreateProjectFromPreviewRequest.builder()
                    .workspaceId(request.getWorkspaceId())
                    .idempotencyKey(request.getIdempotencyKey() != null
                            ? request.getIdempotencyKey()
                            : "demo-" + UUID.nameUUIDFromBytes(
                                    (request.getProjectIdea() + startedAt).getBytes()).toString())
                    .preview(preview)
                    .build();
            CreateProjectFromPreviewResponse created = autonomousProjectCreationService.createFromPreview(createReq);
            stages.get(4).setDurationMs(System.currentTimeMillis() - cStart);
            if (!created.isSuccess()) {
                stages.get(4).setStatus(StageStatus.FAILED);
                stages.get(4).setDetail(created.getError());
                markSkipped(stages, 5);
                return DemoModeResponse.builder()
                        .success(false)
                        .currentStage(DemoStage.CREATING_ENTITIES)
                        .stages(stages)
                        .error(created.getError())
                        .startedAt(startedAt)
                        .finishedAt(LocalDateTime.now())
                        .processingTimeMs(System.currentTimeMillis() - runStart)
                        .steps(steps)
                        .warnings(warnings)
                        .build();
            }
            stages.get(4).setDetail("project=" + created.getProjectName()
                    + ", sprints=" + n(created.getSprintsCreated())
                    + ", tasks=" + n(created.getTasksCreated())
                    + ", subtasks=" + n(created.getSubtasksCreated())
                    + ", assignments=" + n(created.getAssignmentsApplied()));
            stages.get(4).setStatus(StageStatus.DONE);
            steps.add("[Creating] project=" + created.getProjectName()
                    + " sprints=" + n(created.getSprintsCreated())
                    + " tasks=" + n(created.getTasksCreated())
                    + " subtasks=" + n(created.getSubtasksCreated())
                    + " assignments=" + n(created.getAssignmentsApplied()));

            if (created.getWarnings() != null) warnings.addAll(created.getWarnings());

            // ===== 6. REFRESHING_DASHBOARD =====
            markRunning(stages, 5);
            long rStart = System.currentTimeMillis();
            try {
                dashboardEventBroker.publish(new DashboardRefreshEvent(
                        this,
                        request.getWorkspaceId(),
                        "DEMO_MODE_CREATED: " + created.getProjectName(),
                        created.getRefreshSignals() != null
                                ? created.getRefreshSignals()
                                : List.of("projects", "tasks", "dashboard", "members"),
                        created.getProjectId(),
                        created.getProjectName()));
                stages.get(5).setDetail("SSE event published to "
                        + dashboardEventBroker.listenerCount(request.getWorkspaceId()) + " listener(s)");
                stages.get(5).setStatus(StageStatus.DONE);
                steps.add("[Refreshing] SSE event broadcast");
            } catch (Exception ex) {
                stages.get(5).setStatus(StageStatus.FAILED);
                stages.get(5).setDetail(ex.getMessage());
                warnings.add("Dashboard refresh failed: " + ex.getMessage());
            }
            stages.get(5).setDurationMs(System.currentTimeMillis() - rStart);

            // ===== Build final response =====
            Counts counts = Counts.builder()
                    .sprintsCreated(n(created.getSprintsCreated()))
                    .tasksCreated(n(created.getTasksCreated()))
                    .subtasksCreated(n(created.getSubtasksCreated()))
                    .assignmentsApplied(n(created.getAssignmentsApplied()))
                    .risksGenerated(safeSize(preview.getRisks()))
                    .build();

            TimelineInfo timeline = null;
            if (preview.getProject() != null && preview.getProject().getTimeline() != null) {
                PreviewProjectResponse.Timeline t = preview.getProject().getTimeline();
                timeline = TimelineInfo.builder()
                        .startDate(t.getStartDate())
                        .endDate(t.getEndDate())
                        .totalWeeks(t.getTotalWeeks())
                        .totalEstimatedHours(t.getTotalEstimatedHours())
                        .totalStoryPoints(t.getTotalStoryPoints())
                        .build();
            } else if (preview.getEstimatedDuration() != null) {
                timeline = TimelineInfo.builder()
                        .startDate(null)
                        .endDate(null)
                        .totalWeeks(preview.getEstimatedDuration().getWeeksAtTeamSize())
                        .totalEstimatedHours(preview.getEstimatedDuration().getTotalEstimatedHours())
                        .totalStoryPoints(preview.getEstimatedDuration().getTotalStoryPoints())
                        .build();
            }

            return DemoModeResponse.builder()
                    .success(true)
                    .idempotent(created.isIdempotent())
                    .currentStage(DemoStage.COMPLETED)
                    .stages(stages)
                    .counts(counts)
                    .timeline(timeline)
                    .workload(buildWorkload(assignment))
                    .risks(preview.getRisks() != null
                            ? preview.getRisks().stream().map(DemoModeService::riskToString).toList()
                            : List.of())
                    .projectId(created.getProjectId())
                    .projectName(created.getProjectName())
                    .refreshSignals(created.getRefreshSignals())
                    .steps(steps)
                    .warnings(warnings)
                    .startedAt(startedAt)
                    .finishedAt(LocalDateTime.now())
                    .processingTimeMs(System.currentTimeMillis() - runStart)
                    .build();

        } catch (Exception ex) {
            log.error("Demo Mode pipeline failed at {}: {}", findFailedStage(stages), ex.getMessage(), ex);
            return DemoModeResponse.builder()
                    .success(false)
                    .currentStage(findFailedStage(stages))
                    .stages(stages)
                    .steps(steps)
                    .warnings(warnings)
                    .startedAt(startedAt)
                    .finishedAt(LocalDateTime.now())
                    .processingTimeMs(System.currentTimeMillis() - runStart)
                    .error("Pipeline failed: " + ex.getMessage())
                    .build();
        }
    }

    // =================================================================
    // Stage helpers
    // =================================================================

    private PreviewProjectResponse analyze(DemoModeRequest request,
                                            List<DemoStageResult> stages,
                                            List<String> steps) {
        markRunning(stages, 0);
        long start = System.currentTimeMillis();

        // Direct planner call (used to detect zero-task failures early).
        ProjectPlannerResponse plan = projectPlannerService.planProject(
                ProjectPlannerRequest.builder()
                        .projectIdea(request.getProjectIdea())
                        .teamSize(request.getTeamSize())
                        .technologyStack(request.getTechnologyStack())
                        .weeksDeadline(request.getWeeksDeadline() != null
                                ? request.getWeeksDeadline() : 8)
                        .build());
        stages.get(0).setDurationMs(System.currentTimeMillis() - start);
        if (plan.getTasks() == null || plan.getTasks().isEmpty()) {
            stages.get(0).setStatus(StageStatus.FAILED);
            stages.get(0).setDetail("Planner returned no tasks");
            markSkippedAfter(stages, 1);
            return null;
        }
        stages.get(0).setDetail(safeSize(plan.getTasks()) + " tasks, "
                + safeSize(plan.getSubtasks()) + " subtasks, "
                + safeSize(plan.getSprints()) + " sprints, "
                + safeSize(plan.getEpics()) + " epics");
        steps.add("[Analyzing] planner OK (" + safeSize(plan.getTasks())
                + " tasks, " + safeSize(plan.getSubtasks()) + " subtasks)");

        // Then run the preview service which also does assignment in the same call.
        PreviewProjectResponse preview = projectPreviewService.preview(
                PreviewProjectRequest.builder()
                        .projectIdea(request.getProjectIdea())
                        .workspaceId(request.getWorkspaceId())
                        .teamSize(request.getTeamSize())
                        .technologyStack(request.getTechnologyStack())
                        .weeksDeadline(request.getWeeksDeadline() != null
                                ? request.getWeeksDeadline() : 8)
                        .build());
        if (preview.getError() != null) {
            stages.get(0).setStatus(StageStatus.FAILED);
            stages.get(0).setDetail(preview.getError());
            markSkippedAfter(stages, 1);
            return null;
        }
        // Stage 0 stays running until all planner+preview work is done — we mark DONE in caller.
        return preview;
    }

    private com.taskflow.ai.dto.BatchTaskAssignmentRequest buildAssignmentRequest(
            DemoModeRequest request, PreviewProjectResponse preview) {

        // Re-use the preview's tasks. The preview already has assignment, but
        // calling batchRecommendPreview again re-resolves the member ranking
        // and surfaces a fresh workload summary.
        List<com.taskflow.ai.dto.BatchTaskAssignmentRequest.TaskInput> tasks = new ArrayList<>();
        if (preview.getTasks() != null) {
            for (PreviewProjectResponse.TaskPreview t : preview.getTasks()) {
                tasks.add(com.taskflow.ai.dto.BatchTaskAssignmentRequest.TaskInput.builder()
                        .taskRef(t.getId())
                        .title(t.getTitle())
                        .description(t.getDescription())
                        .priority(t.getPriority() != null ? t.getPriority() : "MEDIUM")
                        .dueDate(null)
                        .category(inferCategory(t.getTitle(), t.getDescription()))
                        .build());
            }
        }
        return com.taskflow.ai.dto.BatchTaskAssignmentRequest.builder()
                .projectId(preview.getProject() != null && preview.getPreviewId() != null
                        ? UUID.nameUUIDFromBytes(preview.getPreviewId().getBytes())
                        : UUID.randomUUID())
                .workspaceId(request.getWorkspaceId())
                .tasks(tasks)
                .build();
    }

    private List<WorkloadEntry> buildWorkload(BatchTaskAssignmentResponse assignment) {
        if (assignment.getWorkloadSummary() == null) return List.of();
        List<WorkloadEntry> out = new ArrayList<>();
        for (BatchTaskAssignmentResponse.WorkloadSummary mw : assignment.getWorkloadSummary()) {
            out.add(WorkloadEntry.builder()
                    .memberId(mw.getMemberId())
                    .memberName(mw.getMemberName())
                    .taskCount(mw.getAssignedTaskCount())
                    // The assignment service reports current vs new workload percentages;
                    // expose the new one so the dashboard shows post-creation load.
                    .workloadPercentage(mw.getEstimatedNewWorkloadPercent())
                    .build());
        }
        return out;
    }

    // =================================================================
    // Stage status utilities
    // =================================================================

    private DemoStageResult stage(DemoStage s, String label) {
        return DemoStageResult.builder()
                .stage(s).label(label).status(StageStatus.PENDING).build();
    }

    private void markRunning(List<DemoStageResult> stages, int idx) {
        if (idx < stages.size()) stages.get(idx).setStatus(StageStatus.RUNNING);
    }

    private void markDone(List<DemoStageResult> stages, int idx, String detail) {
        if (idx >= stages.size()) return;
        DemoStageResult s = stages.get(idx);
        s.setStatus(StageStatus.DONE);
        s.setDetail(detail);
    }

    private void markSkipped(List<DemoStageResult> stages, int idx) {
        if (idx < stages.size()) stages.get(idx).setStatus(StageStatus.SKIPPED);
    }

    private void markSkippedAfter(List<DemoStageResult> stages, int fromIdx) {
        for (int i = fromIdx; i < stages.size(); i++) {
            stages.get(i).setStatus(StageStatus.SKIPPED);
        }
    }

    private DemoStage findFailedStage(List<DemoStageResult> stages) {
        for (DemoStageResult s : stages) {
            if (s.getStatus() == StageStatus.RUNNING) return s.getStage();
            if (s.getStatus() == StageStatus.FAILED) return s.getStage();
        }
        return null;
    }

    // =================================================================
    // Generic utilities
    // =================================================================

    private DemoModeResponse fail(List<DemoStageResult> stages, DemoStage failedAt,
                                  String message, LocalDateTime startedAt) {
        markSkippedAfter(stages, 0);
        return DemoModeResponse.builder()
                .success(false)
                .currentStage(failedAt)
                .stages(stages)
                .startedAt(startedAt)
                .finishedAt(LocalDateTime.now())
                .error(message)
                .build();
    }

    private static int safeSize(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private static int n(Integer v) {
        return v == null ? 0 : v;
    }

    private static String inferCategory(String title, String description) {
        String s = ((title == null ? "" : title) + " " + (description == null ? "" : description)).toLowerCase();
        if (s.contains("backend") || s.contains("api") || s.contains("database") || s.contains("server")) return "BACKEND";
        if (s.contains("frontend") || s.contains("react") || s.contains("vue")) return "FRONTEND";
        if (s.contains("ui") || s.contains("ux") || s.contains("design")) return "UI";
        if (s.contains("document") || s.contains("readme") || s.contains("wiki")) return "DOCUMENT";
        if (s.contains("devops") || s.contains("ci/cd") || s.contains("deploy")) return "DEVOPS";
        if (s.contains("test") || s.contains("qa")) return "QA";
        return "OTHER";
    }

    private static String riskToString(PreviewProjectResponse.RiskItem r) {
        if (r == null) return "";
        StringBuilder sb = new StringBuilder();
        if (r.getLevel() != null) sb.append("[").append(r.getLevel()).append("] ");
        if (r.getCategory() != null) sb.append(r.getCategory()).append(": ");
        if (r.getDescription() != null) sb.append(r.getDescription());
        return sb.toString();
    }
}