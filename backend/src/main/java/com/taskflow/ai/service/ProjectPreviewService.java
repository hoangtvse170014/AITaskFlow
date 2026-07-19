package com.taskflow.ai.service;

import com.taskflow.ai.dto.BatchTaskAssignmentRequest;
import com.taskflow.ai.dto.BatchTaskAssignmentResponse;
import com.taskflow.ai.dto.PreviewProjectRequest;
import com.taskflow.ai.dto.PreviewProjectResponse;
import com.taskflow.ai.dto.PreviewProjectResponse.*;
import com.taskflow.ai.dto.ProjectPlannerRequest;
import com.taskflow.ai.dto.ProjectPlannerResponse;
import com.taskflow.ai.dto.ProjectPlannerResponse.Priority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Generates a complete, read-only preview of an AI-generated project:
 *   1. Run project planner AI (no DB writes).
 *   2. Run batch task assignment AI on the planner's tasks (no DB writes).
 *   3. Aggregate: tasks with embedded assignment, sprints, subtasks, risks,
 *      estimated duration, execution order, workload summary, confidence.
 *
 * Nothing is persisted. The user reviews the result and then triggers a
 * separate "create" endpoint (out of scope for this service).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectPreviewService {

    private static final int HOURS_PER_WEEK_PER_MEMBER = 40;
    private static final String PREVIEW_ID_PREFIX = "preview_";

    private final ProjectPlannerService projectPlannerService;
    private final AiTaskAssignmentService taskAssignmentService;

    public PreviewProjectResponse preview(PreviewProjectRequest request) {
        long startTime = System.currentTimeMillis();

        // 1) Generate the plan via the existing planner service (no DB writes).
        ProjectPlannerRequest plannerRequest = ProjectPlannerRequest.builder()
                .projectIdea(request.getProjectIdea())
                .teamSize(request.getTeamSize())
                .technologyStack(request.getTechnologyStack())
                .weeksDeadline(request.getWeeksDeadline())
                .build();

        ProjectPlannerResponse plan = projectPlannerService.planProject(plannerRequest);

        if (plan.getTasks() == null || plan.getTasks().isEmpty()) {
            log.warn("Project planner returned no tasks; aborting preview");
            return PreviewProjectResponse.builder()
                    .previewId(generatePreviewId())
                    .error("Planner returned no tasks: " + plan.getDescription())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        // 2) Convert plan.tasks -> BatchTaskAssignmentRequest (need a fake/draft projectId;
        //    we pass a new random UUID since assignment doesn't actually need to load the
        //    project entity - it only reads workspace members).
        UUID syntheticProjectId = UUID.randomUUID();
        List<BatchTaskAssignmentRequest.TaskInput> assignmentTasks = plan.getTasks().stream()
                .map(t -> BatchTaskAssignmentRequest.TaskInput.builder()
                        .taskRef(t.getId())
                        .title(t.getTitle())
                        .description(t.getDescription())
                        .priority(t.getPriority() != null ? t.getPriority().name() : "MEDIUM")
                        .dueDate(null)
                        .category(inferCategoryFromTitle(t.getTitle(), t.getDescription()))
                        .build())
                .toList();

        BatchTaskAssignmentRequest assignmentRequest = BatchTaskAssignmentRequest.builder()
                .projectId(syntheticProjectId)
                .workspaceId(request.getWorkspaceId())
                .tasks(assignmentTasks)
                .build();

        BatchTaskAssignmentResponse assignment = taskAssignmentService.batchRecommendPreview(
                assignmentRequest, plan.getProjectName());

        // 3) Merge into the preview DTO.
        Map<String, BatchTaskAssignmentResponse.TaskAssignment> assignmentByRef = new HashMap<>();
        if (assignment.getAssignments() != null) {
            for (BatchTaskAssignmentResponse.TaskAssignment a : assignment.getAssignments()) {
                if (a.getTaskRef() != null) {
                    assignmentByRef.put(a.getTaskRef(), a);
                }
            }
        }

        // Index sprints by task id for fast lookup.
        Map<String, ProjectPlannerResponse.Sprint> sprintForTask = new HashMap<>();
        if (plan.getSprints() != null) {
            for (ProjectPlannerResponse.Sprint sprint : plan.getSprints()) {
                if (sprint.getTasks() == null) continue;
                for (String taskRef : sprint.getTasks()) {
                    sprintForTask.put(taskRef, sprint);
                }
            }
        }

        List<TaskPreview> taskPreviews = new ArrayList<>();
        int totalAssignmentConfidenceAccum = 0;
        int assignmentCount = 0;
        for (ProjectPlannerResponse.Task t : plan.getTasks()) {
            BatchTaskAssignmentResponse.TaskAssignment a = assignmentByRef.get(t.getId());
            AssignmentPreview ap = null;
            if (a != null) {
                ap = AssignmentPreview.builder()
                        .assignedMemberId(a.getAssignedMemberId())
                        .assignedMemberName(a.getAssignedMemberName())
                        .roleMatched(a.getRoleMatched())
                        .confidence(a.getConfidence())
                        .reason(a.getReason())
                        .unassigned(a.getUnassigned())
                        .build();
                if (a.getConfidence() != null) {
                    totalAssignmentConfidenceAccum += (int) Math.round(a.getConfidence() * 100);
                    assignmentCount++;
                }
            } else {
                ap = AssignmentPreview.builder()
                        .unassigned(true)
                        .confidence(0.0)
                        .reason("Not included in assignment round")
                        .build();
            }

            ProjectPlannerResponse.Sprint sprint = sprintForTask.get(t.getId());
            taskPreviews.add(TaskPreview.builder()
                    .id(t.getId())
                    .title(t.getTitle())
                    .description(t.getDescription())
                    .epicId(t.getEpicId())
                    .priority(t.getPriority() != null ? t.getPriority().name() : "MEDIUM")
                    .estimatedHours(t.getEstimatedHours())
                    .storyPoints(t.getStoryPoints())
                    .acceptanceCriteria(t.getAcceptanceCriteria())
                    .risks(t.getRisks())
                    .sprintNumber(sprint != null ? sprint.getNumber() : null)
                    .sprintName(sprint != null ? sprint.getName() : null)
                    .assignment(ap)
                    .build());
        }

        // Sprint summaries with aggregated stats.
        List<SprintSummary> sprintSummaries = new ArrayList<>();
        if (plan.getSprints() != null) {
            Map<String, ProjectPlannerResponse.Task> taskById = new HashMap<>();
            for (ProjectPlannerResponse.Task t : plan.getTasks()) {
                taskById.put(t.getId(), t);
            }
            for (ProjectPlannerResponse.Sprint s : plan.getSprints()) {
                int estimatedHours = 0;
                int taskCount = 0;
                if (s.getTasks() != null) {
                    for (String ref : s.getTasks()) {
                        ProjectPlannerResponse.Task t = taskById.get(ref);
                        if (t != null) {
                            estimatedHours += t.getEstimatedHours() != null ? t.getEstimatedHours() : 0;
                            taskCount++;
                        }
                    }
                }
                sprintSummaries.add(SprintSummary.builder()
                        .number(s.getNumber())
                        .name(s.getName())
                        .goal(s.getGoal())
                        .startDate(s.getStartDate())
                        .endDate(s.getEndDate())
                        .taskIds(s.getTasks())
                        .capacity(s.getCapacity())
                        .estimatedHours(estimatedHours)
                        .taskCount(taskCount)
                        .build());
            }
        }

        // Subtask list (flat, easy to render).
        List<SubtaskPreview> subtaskPreviews = new ArrayList<>();
        if (plan.getSubtasks() != null) {
            for (ProjectPlannerResponse.Subtask st : plan.getSubtasks()) {
                subtaskPreviews.add(SubtaskPreview.builder()
                        .id(st.getId())
                        .parentTaskId(st.getParentTaskId())
                        .title(st.getTitle())
                        .estimatedHours(st.getEstimatedHours())
                        .suggestedAssignee(st.getSuggestedAssignee())
                        .build());
            }
        }

        // Risk aggregation.
        List<RiskItem> risks = aggregateRisks(plan.getTasks(), plan.getDependencies());

        // Estimated duration.
        EstimatedDuration duration = estimateDuration(
                plan.getTasks(),
                plan.getSubtasks(),
                plan.getTimeline(),
                request.getTeamSize());

        // Execution order (topological sort on dependencies).
        List<String> executionOrder = computeExecutionOrder(plan.getTasks(), plan.getDependencies());

        // Project section.
        ProjectPreview projectSection = ProjectPreview.builder()
                .name(plan.getProjectName())
                .description(plan.getDescription())
                .scope(plan.getScope())
                .weeksDeadline(request.getWeeksDeadline())
                .technologyStack(request.getTechnologyStack())
                .teamSize(request.getTeamSize())
                .timeline(plan.getTimeline() != null ? Timeline.builder()
                        .startDate(plan.getTimeline().getStartDate())
                        .endDate(plan.getTimeline().getEndDate())
                        .totalWeeks(plan.getTimeline().getTotalWeeks())
                        .totalEstimatedHours(plan.getTimeline().getTotalEstimatedHours())
                        .totalStoryPoints(plan.getTimeline().getTotalStoryPoints())
                        .build() : null)
                .milestones(plan.getMilestones() == null ? List.of() : plan.getMilestones().stream()
                        .map(m -> MilestonePreview.builder()
                                .name(m.getName())
                                .description(m.getDescription())
                                .targetDate(m.getTargetDate())
                                .deliverables(m.getDeliverables())
                                .build())
                        .toList())
                .epics(plan.getEpics() == null ? List.of() : plan.getEpics().stream()
                        .map(e -> EpicPreview.builder()
                                .id(e.getId())
                                .name(e.getName())
                                .description(e.getDescription())
                                .priority(e.getPriority() != null ? e.getPriority().name() : "MEDIUM")
                                .estimatedHours(e.getEstimatedHours())
                                .features(e.getFeatures())
                                .build())
                        .toList())
                .build();

        // Combined warnings.
        List<String> warnings = new ArrayList<>();
        if (assignment.getWarnings() != null) warnings.addAll(assignment.getWarnings());
        if (assignment.getError() != null) warnings.add("Assignment: " + assignment.getError());
        if (plan.getTasks() != null && plan.getTasks().size() > 30) {
            warnings.add("Plan has " + plan.getTasks().size() + " tasks; consider trimming scope.");
        }
        if (duration.getWeeksAtTeamSize() != null && request.getWeeksDeadline() != null
                && duration.getWeeksAtTeamSize() > request.getWeeksDeadline()) {
            warnings.add("Estimated duration (" + duration.getWeeksAtTeamSize()
                    + " weeks) exceeds requested deadline (" + request.getWeeksDeadline() + " weeks).");
        }

        double overallAssignmentConfidence = assignmentCount == 0 ? 0.0 :
                (totalAssignmentConfidenceAccum / 100.0) / assignmentCount;
        double planningConfidence = plan.getConfidence() != null ? plan.getConfidence() : 0.0;
        double overall = (overallAssignmentConfidence + planningConfidence) / 2.0;

        long processingTime = System.currentTimeMillis() - startTime;

        return PreviewProjectResponse.builder()
                .previewId(generatePreviewId())
                .project(projectSection)
                .sprints(sprintSummaries)
                .tasks(taskPreviews)
                .subtasks(subtaskPreviews)
                .executionOrder(executionOrder)
                .estimatedDuration(duration)
                .risks(risks)
                .workloadSummary(assignment.getWorkloadSummary())
                .overallAssignmentConfidence(overallAssignmentConfidence)
                .planningConfidence(planningConfidence)
                .overallConfidence(overall)
                .warnings(warnings)
                .reason(assignment.getReason() != null ? assignment.getReason()
                        : "Preview generated. Review then approve to create.")
                .processingTimeMs(processingTime)
                .build();
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private List<RiskItem> aggregateRisks(
            List<ProjectPlannerResponse.Task> tasks,
            List<ProjectPlannerResponse.Dependency> deps) {
        List<RiskItem> risks = new ArrayList<>();
        if (tasks != null) {
            for (ProjectPlannerResponse.Task t : tasks) {
                if (t.getRisks() == null) continue;
                for (String r : t.getRisks()) {
                    if (r == null || r.isBlank()) continue;
                    risks.add(RiskItem.builder()
                            .level(t.getPriority() == Priority.CRITICAL || t.getPriority() == Priority.HIGH
                                    ? "HIGH" : "MEDIUM")
                            .category("TASK")
                            .description(r)
                            .source(t.getId())
                            .build());
                }
            }
        }
        if (deps != null) {
            for (ProjectPlannerResponse.Dependency d : deps) {
                risks.add(RiskItem.builder()
                        .level("MEDIUM")
                        .category("DEPENDENCY")
                        .description("Dependency: " + d.getTaskId() + " blocked by " + d.getDependsOnTaskId()
                                + " (" + d.getType() + ")" + (d.getReason() != null ? " - " + d.getReason() : ""))
                        .source(d.getTaskId())
                        .build());
            }
        }
        // Deduplicate by description.
        Map<String, RiskItem> dedup = new java.util.LinkedHashMap<>();
        for (RiskItem ri : risks) {
            dedup.putIfAbsent(ri.getDescription(), ri);
        }
        return new ArrayList<>(dedup.values());
    }

    private EstimatedDuration estimateDuration(
            List<ProjectPlannerResponse.Task> tasks,
            List<ProjectPlannerResponse.Subtask> subtasks,
            ProjectPlannerResponse.Timeline timeline,
            String teamSize) {

        int taskHours = 0;
        int subtaskHours = 0;
        int taskCount = tasks == null ? 0 : tasks.size();
        int subtaskCount = subtasks == null ? 0 : subtasks.size();
        int storyPoints = 0;

        if (tasks != null) {
            for (ProjectPlannerResponse.Task t : tasks) {
                taskHours += t.getEstimatedHours() != null ? t.getEstimatedHours() : 0;
                storyPoints += t.getStoryPoints() != null ? t.getStoryPoints() : 0;
            }
        }
        if (subtasks != null) {
            for (ProjectPlannerResponse.Subtask st : subtasks) {
                subtaskHours += st.getEstimatedHours() != null ? st.getEstimatedHours() : 0;
            }
        }

        // Subtasks overlap with task hours; take the larger of the two.
        int totalHours = Math.max(taskHours, taskHours + (subtaskHours - Math.min(subtaskHours, taskHours / 4)));

        int members = parseTeamSize(teamSize);
        if (members <= 0) members = 1;

        int weeklyCapacity = members * HOURS_PER_WEEK_PER_MEMBER;
        int weeksAtTeamSize = weeklyCapacity == 0 ? 0 : (int) Math.ceil((double) totalHours / weeklyCapacity);

        // If the planner already gave a timeline, prefer the larger value as a sanity floor.
        if (timeline != null && timeline.getTotalWeeks() != null && timeline.getTotalWeeks() > weeksAtTeamSize) {
            weeksAtTeamSize = timeline.getTotalWeeks();
        }

        return EstimatedDuration.builder()
                .totalTasks(taskCount)
                .totalSubtasks(subtaskCount)
                .totalEstimatedHours(totalHours)
                .totalStoryPoints(storyPoints)
                .weeksAtTeamSize(weeksAtTeamSize)
                .teamSize(teamSize != null ? teamSize : String.valueOf(members))
                .hoursPerWeekPerMember(HOURS_PER_WEEK_PER_MEMBER)
                .teamMemberCount(members)
                .build();
    }

    private int parseTeamSize(String teamSize) {
        if (teamSize == null || teamSize.isBlank()) return 0;
        try {
            // Try to extract the first integer in the string (e.g. "5" or "team of 5 devs").
            StringBuilder sb = new StringBuilder();
            for (char c : teamSize.toCharArray()) {
                if (Character.isDigit(c)) sb.append(c);
                else if (sb.length() > 0) break;
            }
            return sb.length() == 0 ? 0 : Integer.parseInt(sb.toString());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /**
     * Topological order (Kahn's algorithm). Tasks not connected to any dependency
     * appear in input order. Cycles are broken by ignoring back-edges.
     */
    private List<String> computeExecutionOrder(
            List<ProjectPlannerResponse.Task> tasks,
            List<ProjectPlannerResponse.Dependency> deps) {

        if (tasks == null || tasks.isEmpty()) return List.of();

        Map<String, List<String>> outgoing = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        for (ProjectPlannerResponse.Task t : tasks) {
            outgoing.computeIfAbsent(t.getId(), k -> new ArrayList<>());
            inDegree.putIfAbsent(t.getId(), 0);
        }
        if (deps != null) {
            for (ProjectPlannerResponse.Dependency d : deps) {
                if (d.getTaskId() == null || d.getDependsOnTaskId() == null) continue;
                if (!inDegree.containsKey(d.getTaskId()) || !inDegree.containsKey(d.getDependsOnTaskId())) continue;
                outgoing.get(d.getDependsOnTaskId()).add(d.getTaskId());
                inDegree.merge(d.getTaskId(), 1, Integer::sum);
            }
        }

        Map<String, Integer> inputOrder = new HashMap<>();
        for (int i = 0; i < tasks.size(); i++) {
            inputOrder.put(tasks.get(i).getId(), i);
        }

        java.util.TreeSet<String> ready = new java.util.TreeSet<>(
                Comparator.comparingInt(id -> inputOrder.getOrDefault(id, Integer.MAX_VALUE)));
        for (Map.Entry<String, Integer> e : inDegree.entrySet()) {
            if (e.getValue() == 0) ready.add(e.getKey());
        }

        List<String> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            String next = ready.pollFirst();
            order.add(next);
            for (String child : outgoing.getOrDefault(next, List.of())) {
                int newDeg = inDegree.merge(child, -1, Integer::sum);
                if (newDeg == 0) ready.add(child);
            }
        }

        // If a cycle prevented some tasks from being scheduled, append them in input order.
        if (order.size() < tasks.size()) {
            Set<String> ordered = new HashSet<>(order);
            for (ProjectPlannerResponse.Task t : tasks) {
                if (!ordered.contains(t.getId())) order.add(t.getId());
            }
        }
        return order;
    }

    private String inferCategoryFromTitle(String title, String description) {
        if (title == null && description == null) return "OTHER";
        String s = ((title == null ? "" : title) + " " + (description == null ? "" : description)).toLowerCase();
        if (s.contains("backend") || s.contains("api") || s.contains("database") || s.contains("server")
                || s.contains("spring") || s.contains("auth")) return "BACKEND";
        if (s.contains("frontend") || s.contains("react") || s.contains("vue") || s.contains("angular")) return "FRONTEND";
        if (s.contains("ui") || s.contains("ux") || s.contains("design") || s.contains("wireframe")) return "UI";
        if (s.contains("document") || s.contains("readme") || s.contains("wiki") || s.contains("spec")) return "DOCUMENT";
        if (s.contains("devops") || s.contains("ci/cd") || s.contains("deploy") || s.contains("docker")) return "DEVOPS";
        if (s.contains("test") || s.contains("qa")) return "QA";
        return "OTHER";
    }

    private String generatePreviewId() {
        return PREVIEW_ID_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}