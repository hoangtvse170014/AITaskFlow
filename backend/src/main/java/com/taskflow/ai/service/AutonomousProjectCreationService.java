package com.taskflow.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.ai.dto.CreateProjectFromPreviewRequest;
import com.taskflow.ai.dto.CreateProjectFromPreviewResponse;
import com.taskflow.ai.dto.PreviewProjectResponse;
import com.taskflow.dto.request.CreateProjectRequest;
import com.taskflow.dto.request.CreateTaskRequest;
import com.taskflow.dto.request.SubTaskRequest;
import com.taskflow.dto.request.UpdateTaskRequest;
import com.taskflow.dto.response.ProjectResponse;
import com.taskflow.dto.response.SubTaskResponse;
import com.taskflow.dto.response.TaskResponse;
import com.taskflow.event.DashboardEventBroker;
import com.taskflow.event.DashboardRefreshEvent;
import com.taskflow.repository.WorkspaceMemberRepository;
import com.taskflow.service.ProjectService;
import com.taskflow.service.SubTaskService;
import com.taskflow.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Autonomous project creation. Given a PreviewProjectResponse, this service
 * sequentially drives the existing Project/Task/SubTask APIs to materialize
 * the project, sprints (encoded as task labels), tasks, subtasks, and
 * assignments. On any failure it rolls back in reverse order.
 *
 * No duplicate creation: an idempotency key derived from the preview payload
 * is checked against existing projects' descriptions before any insert runs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutonomousProjectCreationService {

    private static final String SPRINT_LABEL_PREFIX = "sprint:";
    private static final String PREVIEW_MARKER = "[AI-PREVIEW:";
    private static final Pattern PROJECT_KEY_PATTERN = Pattern.compile("^[A-Z]{2,6}$");

    private final ProjectService projectService;
    private final TaskService taskService;
    private final SubTaskService subTaskService;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final DashboardEventBroker dashboardEventBroker;

    public CreateProjectFromPreviewResponse createFromPreview(CreateProjectFromPreviewRequest request) {
        long startTime = System.currentTimeMillis();
        List<String> steps = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> createdProjects = new ArrayList<>();   // LIFO rollback
        List<UUID> createdTasks = new ArrayList<>();        // LIFO rollback

        PreviewProjectResponse preview = request.getPreview();
        if (preview == null || preview.getProject() == null) {
            return CreateProjectFromPreviewResponse.fail("Preview payload is empty");
        }

        UUID workspaceId = request.getWorkspaceId();
        String idempotencyKey = computeIdempotencyKey(preview, request.getIdempotencyKey());

        log.info("Starting autonomous project creation: workspaceId={}, previewId={}, idempotencyKey={}",
                workspaceId, preview.getPreviewId(), idempotencyKey);

        // ---------- Idempotency check ----------
        Optional<UUID> existing = findExistingProject(workspaceId, idempotencyKey);
        if (existing.isPresent()) {
            UUID projectId = existing.get();
            log.info("Idempotent hit: project {} already exists for key {}", projectId, idempotencyKey);
            ProjectResponse pr = projectService.getProjectById(workspaceId, projectId);
            return CreateProjectFromPreviewResponse.builder()
                    .success(true)
                    .idempotent(true)
                    .projectId(projectId)
                    .projectName(pr.getName())
                    .steps(List.of("Idempotent: project " + projectId + " already exists"))
                    .warnings(warnings)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .refreshSignals(List.of("workspaces", "projects", "tasks", "dashboard"))
                    .build();
        }

        UUID projectId;
        try {
            // ---------- Step 1: Project ----------
            CreateProjectRequest projectReq = CreateProjectRequest.builder()
                    .name(preview.getProject().getName())
                    .key(deriveProjectKey(preview.getProject().getName()))
                    .description(buildProjectDescription(preview, idempotencyKey))
                    .color("#6366f1")
                    .icon("🤖")
                    .build();

            ProjectResponse created = projectService.createProject(workspaceId, projectReq);
            projectId = UUID.fromString(created.getId());
            createdProjects.add(projectId.toString());
            steps.add("Created project: " + created.getName() + " (" + projectId + ")");

            // ---------- Step 2: Tasks (with sprint as label) ----------
            int subtasksCreated = 0;
            int assignmentsApplied = 0;
            int sprintsCreated = 0;
            int tasksCreated = 0;

            if (preview.getTasks() != null) {
                // Group tasks by sprint to count sprints created
                java.util.Set<String> sprintSet = new java.util.HashSet<>();
                for (PreviewProjectResponse.TaskPreview tp : preview.getTasks()) {
                    if (tp.getSprintName() != null) sprintSet.add(tp.getSprintName());
                }
                sprintsCreated = sprintSet.size();

                for (PreviewProjectResponse.TaskPreview tp : preview.getTasks()) {
                    try {
                        // Build sprint label
                        List<CreateTaskRequest.LabelDto> labels = new ArrayList<>();
                        if (tp.getSprintName() != null) {
                            labels.add(sprintLabel(tp.getSprintNumber(), tp.getSprintName()));
                        }
                        if (tp.getEpicId() != null) {
                            labels.add(epicLabel(tp.getEpicId()));
                        }

                        CreateTaskRequest taskReq = CreateTaskRequest.builder()
                                .title(truncate(tp.getTitle(), 255))
                                .description(tp.getDescription())
                                .status("TODO")
                                .priority(normalizePriority(tp.getPriority()))
                                .assigneeId(extractUserId(tp))
                                .labels(labels)
                                .checklist(buildChecklistFromAcceptance(tp))
                                .build();

                        TaskResponse tr = taskService.createTask(projectId, taskReq);
                        UUID taskId = UUID.fromString(tr.getId());
                        createdTasks.add(taskId);
                        tasksCreated++;
                        steps.add("Created task: " + tp.getId() + " -> " + tr.getId());

                        // ---------- Step 3: Subtasks ----------
                        if (preview.getSubtasks() != null) {
                            List<PreviewProjectResponse.SubtaskPreview> subs = preview.getSubtasks().stream()
                                    .filter(s -> tp.getId().equals(s.getParentTaskId()))
                                    .toList();
                            for (PreviewProjectResponse.SubtaskPreview st : subs) {
                                try {
                                    SubTaskRequest subReq = SubTaskRequest.builder()
                                            .title(truncate(st.getTitle(), 255))
                                            .build();
                                    SubTaskResponse subRes = subTaskService.createSubTask(taskId, subReq);
                                    subtasksCreated++;
                                    steps.add("  + subtask: " + st.getId() + " -> " + subRes.getId());
                                } catch (Exception subEx) {
                                    log.warn("Subtask creation failed for {}: {}", st.getId(), subEx.getMessage());
                                    warnings.add("Subtask " + st.getId() + " failed: " + subEx.getMessage());
                                }
                            }
                        }

                        // ---------- Step 4: Assignment (re-apply via update if not set) ----------
                        if (tp.getAssignment() != null
                                && tp.getAssignment().getAssignedMemberId() != null
                                && Boolean.FALSE.equals(tp.getAssignment().getUnassigned())) {
                            try {
                                // Resolve assignee memberId -> userId (the API expects user id)
                                UUID assigneeUserId = resolveUserIdFromMemberId(
                                        tp.getAssignment().getAssignedMemberId());
                                if (assigneeUserId != null) {
                                    UpdateTaskRequest upd = UpdateTaskRequest.builder()
                                            .assigneeId(assigneeUserId.toString())
                                            .build();
                                    taskService.updateTask(projectId, taskId, upd);
                                    assignmentsApplied++;
                                    steps.add("  = assigned: " + tp.getAssignment().getAssignedMemberName()
                                            + " -> " + tr.getId());
                                }
                            } catch (Exception asgEx) {
                                log.warn("Assignment update failed for {}: {}", tp.getId(), asgEx.getMessage());
                                warnings.add("Assignment for " + tp.getId() + " failed: " + asgEx.getMessage());
                            }
                        }

                    } catch (Exception taskEx) {
                        log.error("Task creation failed for {}: {}", tp.getId(), taskEx.getMessage());
                        throw new RuntimeException("Task creation failed for " + tp.getId() + ": "
                                + taskEx.getMessage(), taskEx);
                    }
                }
            }

            CreateProjectFromPreviewResponse response = CreateProjectFromPreviewResponse.builder()
                    .success(true)
                    .idempotent(false)
                    .projectId(projectId)
                    .projectName(preview.getProject().getName())
                    .sprintsCreated(sprintsCreated)
                    .tasksCreated(tasksCreated)
                    .subtasksCreated(subtasksCreated)
                    .assignmentsApplied(assignmentsApplied)
                    .steps(steps)
                    .warnings(warnings)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .refreshSignals(List.of("workspaces", "projects", "tasks", "dashboard", "members"))
                    .build();

            // Notify any connected dashboards via SSE so the UI refreshes itself.
            dashboardEventBroker.publish(new DashboardRefreshEvent(
                    this,
                    workspaceId,
                    "PROJECT_CREATED_FROM_PREVIEW: " + preview.getProject().getName()
                            + " (" + tasksCreated + " tasks, " + subtasksCreated + " subtasks)",
                    response.getRefreshSignals(),
                    projectId,
                    preview.getProject().getName()));

            return response;

        } catch (Exception ex) {
            log.error("Autonomous creation failed, rolling back. Reason: {}", ex.getMessage(), ex);
            rollback(createdProjects, createdTasks, workspaceId);
            return CreateProjectFromPreviewResponse.builder()
                    .success(false)
                    .error("Creation failed (rolled back): " + ex.getMessage())
                    .steps(steps)
                    .warnings(warnings)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .refreshSignals(List.of())
                    .build();
        }
    }

    // ---------------------------------------------------------------------
    // Rollback
    // ---------------------------------------------------------------------

    private void rollback(List<String> createdProjectIds, List<UUID> createdTaskIds, UUID workspaceId) {
        // Delete tasks first (LIFO), then project.
        for (int i = createdTaskIds.size() - 1; i >= 0; i--) {
            UUID taskId = createdTaskIds.get(i);
            try {
                // Need projectId - all tasks belong to the same projectId in this run
                if (!createdProjectIds.isEmpty()) {
                    UUID projectId = UUID.fromString(createdProjectIds.get(0));
                    taskService.deleteTask(projectId, taskId);
                    log.info("Rollback: deleted task {}", taskId);
                }
            } catch (Exception ex) {
                log.warn("Rollback: failed to delete task {}: {}", taskId, ex.getMessage());
            }
        }
        for (int i = createdProjectIds.size() - 1; i >= 0; i--) {
            UUID projectId = UUID.fromString(createdProjectIds.get(i));
            try {
                projectService.deleteProject(workspaceId, projectId);
                log.info("Rollback: deleted project {}", projectId);
            } catch (Exception ex) {
                log.warn("Rollback: failed to delete project {}: {}", projectId, ex.getMessage());
            }
        }
    }

    // ---------------------------------------------------------------------
    // Idempotency
    // ---------------------------------------------------------------------

    private Optional<UUID> findExistingProject(UUID workspaceId, String idempotencyKey) {
        try {
            List<ProjectResponse> all = projectService.getAllProjects(workspaceId);
            String marker = PREVIEW_MARKER + idempotencyKey + "]";
            for (ProjectResponse p : all) {
                if (p.getDescription() != null && p.getDescription().contains(marker)) {
                    return Optional.of(UUID.fromString(p.getId()));
                }
            }
        } catch (Exception ex) {
            log.warn("Idempotency lookup failed: {}", ex.getMessage());
        }
        return Optional.empty();
    }

    private String computeIdempotencyKey(PreviewProjectResponse preview, String provided) {
        if (provided != null && !provided.isBlank()) {
            return sanitizeKey(provided);
        }
        // Try previewId first
        if (preview.getPreviewId() != null && !preview.getPreviewId().isBlank()) {
            return sanitizeKey(preview.getPreviewId());
        }
        // Hash of canonical payload
        try {
            String canonical = canonicalize(preview);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 32);
        } catch (Exception ex) {
            return "fallback-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }

    private String canonicalize(PreviewProjectResponse p) {
        StringBuilder sb = new StringBuilder();
        if (p.getProject() != null) sb.append(p.getProject().getName()).append('|');
        if (p.getTasks() != null) {
            for (PreviewProjectResponse.TaskPreview t : p.getTasks()) {
                sb.append(t.getId()).append(':').append(t.getTitle()).append(';');
            }
        }
        if (p.getSubtasks() != null) {
            for (PreviewProjectResponse.SubtaskPreview s : p.getSubtasks()) {
                sb.append(s.getId()).append(':').append(s.getParentTaskId()).append(':').append(s.getTitle()).append(';');
            }
        }
        return sb.toString();
    }

    private String sanitizeKey(String raw) {
        return raw.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private String buildProjectDescription(PreviewProjectResponse preview, String idempotencyKey) {
        StringBuilder sb = new StringBuilder();
        if (preview.getProject().getDescription() != null) {
            sb.append(preview.getProject().getDescription());
        }
        sb.append("\n\n").append(PREVIEW_MARKER).append(idempotencyKey).append("]");
        if (preview.getProject().getScope() != null) {
            sb.append("\nScope: ").append(preview.getProject().getScope());
        }
        return truncate(sb.toString(), 500);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private String deriveProjectKey(String name) {
        if (name == null || name.isBlank()) return "AI" + (System.currentTimeMillis() % 1000);
        StringBuilder sb = new StringBuilder();
        for (char c : name.toUpperCase(Locale.ROOT).toCharArray()) {
            if (c >= 'A' && c <= 'Z') sb.append(c);
            if (sb.length() == 6) break;
        }
        if (sb.length() < 2) sb.append("AI");
        String key = sb.toString();
        if (!PROJECT_KEY_PATTERN.matcher(key).matches()) {
            key = "AI";
        }
        return key;
    }

    private CreateTaskRequest.LabelDto sprintLabel(Integer number, String name) {
        String labelName = SPRINT_LABEL_PREFIX + (number != null ? number : "?") + " " + name;
        return CreateTaskRequest.LabelDto.builder()
                .id("sprint-" + number)
                .name(labelName)
                .color("#10b981")
                .build();
    }

    private CreateTaskRequest.LabelDto epicLabel(String epicId) {
        return CreateTaskRequest.LabelDto.builder()
                .id("epic-" + epicId)
                .name("epic:" + epicId)
                .color("#6366f1")
                .build();
    }

    private String normalizePriority(String p) {
        if (p == null) return "MEDIUM";
        return switch (p.toUpperCase(Locale.ROOT)) {
            case "CRITICAL", "URGENT" -> "CRITICAL";
            case "HIGH" -> "HIGH";
            case "LOW" -> "LOW";
            default -> "MEDIUM";
        };
    }

    private String extractUserId(PreviewProjectResponse.TaskPreview tp) {
        // The assignment service returns member UUID (workspace_member_id) but TaskService
        // expects user UUID. Resolve here so creation includes the assignee directly.
        if (tp.getAssignment() == null) return null;
        if (Boolean.TRUE.equals(tp.getAssignment().getUnassigned())) return null;
        UUID userId = resolveUserIdFromMemberId(tp.getAssignment().getAssignedMemberId());
        return userId == null ? null : userId.toString();
    }

    /**
     * Resolves a workspaceMemberId to the underlying user id by hitting the DB.
     * Falls back to returning the raw id (in which case assignment will be re-applied
     * by an explicit update step).
     */
    private UUID resolveUserIdFromMemberId(String memberId) {
        if (memberId == null) return null;
        try {
            UUID mid = UUID.fromString(memberId);
            return workspaceMemberRepository.findUserIdByMemberId(mid).orElse(null);
        } catch (Exception ex) {
            log.debug("resolveUserIdFromMemberId: {} -> {}", memberId, ex.getMessage());
            return null;
        }
    }

    private List<CreateTaskRequest.ChecklistItemDto> buildChecklistFromAcceptance(PreviewProjectResponse.TaskPreview tp) {
        if (tp.getAcceptanceCriteria() == null || tp.getAcceptanceCriteria().isEmpty()) return null;
        List<CreateTaskRequest.ChecklistItemDto> list = new ArrayList<>();
        for (String c : tp.getAcceptanceCriteria()) {
            if (c == null || c.isBlank()) continue;
            list.add(CreateTaskRequest.ChecklistItemDto.builder()
                    .id(UUID.randomUUID().toString())
                    .text(truncate(c, 250))
                    .completed(false)
                    .build());
        }
        return list.isEmpty() ? null : list;
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max - 1) + "\u2026";
    }
}