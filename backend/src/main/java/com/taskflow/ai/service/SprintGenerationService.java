package com.taskflow.ai.service;

import com.taskflow.ai.dto.SprintGenerateRequest;
import com.taskflow.ai.dto.SprintGenerateResponse;
import com.taskflow.ai.dto.SprintGenerateResponse.*;
import com.taskflow.ai.dto.WorkspaceSnapshot;
import com.taskflow.entity.*;
import com.taskflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dedicated sprint generation service.
 * Uses the same workspace snapshot pipeline as the WorkspaceAssistant
 * but returns a structured {@link SprintGenerateResponse} instead of a
 * free-form answer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SprintGenerationService {

    private final WorkspaceRepository workspaceRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final SubTaskRepository subTaskRepository;
    private final GoalRepository goalRepository;
    private final PageRepository pageRepository;
    private final BlockRepository blockRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final MemberWorkloadRepository workloadRepository;
    private final ActivityLogRepository activityLogRepository;
    private final TaskCommentRepository commentRepository;

    @Transactional(readOnly = true)
    public SprintGenerateResponse generateSprint(SprintGenerateRequest request) {
        long start = System.currentTimeMillis();
        try {
            WorkspaceSnapshot snapshot = loadSnapshot(request.getWorkspaceId());

            List<SprintTask> tasks = buildSprintTasks(snapshot, request);
            TeamCapacity capacity = buildCapacity(snapshot);
            String goal = buildSprintGoal(tasks, snapshot);
            List<String> risks = identifyRisks(tasks, snapshot);
            List<String> suggestions = buildSuggestions(tasks, capacity);

            return SprintGenerateResponse.builder()
                    .sprintGoal(goal)
                    .tasks(tasks)
                    .capacity(capacity)
                    .risks(risks)
                    .suggestions(suggestions)
                    .confidence(0.85)
                    .processingTimeMs(System.currentTimeMillis() - start)
                    .build();
        } catch (Exception e) {
            log.error("Sprint generation failed", e);
            return SprintGenerateResponse.builder()
                    .error("Sprint generation failed: " + e.getMessage())
                    .confidence(0.0)
                    .processingTimeMs(System.currentTimeMillis() - start)
                    .build();
        }
    }

    @Transactional(readOnly = true)
    protected WorkspaceSnapshot loadSnapshot(java.util.UUID workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        List<Project> projects = projectRepository.findAllByWorkspaceId(workspaceId);
        List<Task> tasks = taskRepository.findAllByWorkspaceIdEager(workspaceId);
        List<SubTask> subtasks = subTaskRepository.findAllByWorkspaceId(workspaceId);
        List<Goal> goals = goalRepository.findActiveGoalsByWorkspace(workspaceId);
        if (goals.isEmpty()) goals = goalRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        List<Page> pages = pageRepository.findAllActivePagesByWorkspace(workspaceId);
        if (pages.isEmpty()) pages = pageRepository.findAllByWorkspaceIdOrderBySidebarOrder(workspaceId);
        List<Block> recentBlocks = blockRepository.findRecentTextBlocksByWorkspaceId(workspaceId);
        List<WorkspaceMember> members = memberRepository.findAllActiveByWorkspaceId(workspaceId);
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);
        List<MemberWorkload> workloads = workloadRepository.findByWorkspaceIdAndDateRange(workspaceId, weekAgo, today);
        List<ActivityLog> activities = activityLogRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        List<TaskComment> comments = commentRepository.findRecentByWorkspaceId(workspaceId);

        return WorkspaceSnapshot.builder()
                .workspace(workspace)
                .projects(projects)
                .tasks(tasks)
                .subtasks(subtasks)
                .goals(goals)
                .pages(pages)
                .recentBlocks(recentBlocks)
                .members(members)
                .workloads(workloads)
                .recentActivities(activities)
                .recentComments(comments)
                .today(today)
                .build();
    }

    private List<SprintTask> buildSprintTasks(WorkspaceSnapshot snapshot, SprintGenerateRequest req) {
        List<Task> allTasks = snapshot.getTasks() != null ? snapshot.getTasks() : List.<Task>of();
        List<Task> pool = allTasks.stream()
                .filter(t -> t.getStatus() != TaskStatus.DONE || Boolean.TRUE.equals(req.getIncludeDone()))
                .sorted(Comparator.<Task>comparingInt(t -> priorityRank(t.getPriority()))
                        .thenComparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(req.getMaxTasks() != null ? req.getMaxTasks() : 10)
                .toList();

        Map<UUID, MemberWorkload> loadMap = snapshot.getWorkloads() != null
                ? snapshot.getWorkloads().stream()
                    .collect(Collectors.toMap(w -> w.getMember().getId(), w -> w, (a, b) -> a))
                : Map.of();

        SkillResolver skillResolver = new SkillResolver();
        List<SprintTask> sprintTasks = new ArrayList<>();

        for (Task t : pool) {
            WorkspaceMember bestAssignee = findBestAssignee(t, snapshot.getMembers(), loadMap, skillResolver);
            String effort = estimateEffort(t);

            sprintTasks.add(SprintTask.builder()
                    .taskId(t.getId() != null ? t.getId().toString() : null)
                    .taskKey(t.getTaskKey())
                    .title(t.getTitle())
                    .projectName(t.getProject() != null ? t.getProject().getName() : null)
                    .priority(t.getPriority().name())
                    .status(t.getStatus().name())
                    .dueDate(t.getDueDate() != null ? t.getDueDate().toString() : null)
                    .suggestedAssigneeId(bestAssignee != null && bestAssignee.getId() != null
                            ? bestAssignee.getId().toString() : null)
                    .suggestedAssigneeName(bestAssignee != null && bestAssignee.getUser() != null
                            ? bestAssignee.getUser().getFullName() : null)
                    .effort(effort)
                    .reason(buildTaskReason(t, bestAssignee, loadMap))
                    .build());
        }
        return sprintTasks;
    }

    private WorkspaceMember findBestAssignee(Task task,
                                          List<WorkspaceMember> members,
                                          Map<UUID, MemberWorkload> loadMap,
                                          SkillResolver skillResolver) {
        if (members == null || members.isEmpty()) return null;

        String skill = skillResolver.detectSkill(task.getTitle(), task.getDescription(), null);
        record Candidate(WorkspaceMember m, int score, String reason) {}
        List<Candidate> candidates = new ArrayList<>();

        for (WorkspaceMember m : members) {
            if (m.getUser() == null) continue;
            int score = 100;
            String reason = "";

            MemberWorkload w = loadMap.get(m.getId());
            int pct = w != null && w.getWorkloadPercentage() != null ? w.getWorkloadPercentage() : 0;
            if (pct > 90) { score -= 60; reason += "overloaded;"; }
            else if (pct > 70) { score -= 20; reason += "busy;"; }
            else if (pct < 40) { score += 15; reason += "capacity available;"; }

            if (m.getRole() != null && skillResolver.roleMatchesSkill(m.getRole().getName(), skill)) {
                score += 25; reason += "skill match;"; }

            candidates.add(new Candidate(m, score, reason.trim()));
        }

        candidates.sort((a, b) -> Integer.compare(b.score(), a.score()));
        return candidates.isEmpty() ? null : candidates.get(0).m();
    }

    private String estimateEffort(Task task) {
        int len = task.getDescription() != null ? task.getDescription().length() : 0;
        int checklist = task.getChecklist() != null ? task.getChecklist().size() : 0;
        if (len > 500 || checklist > 5) return "L";
        if (len > 150 || checklist > 2) return "M";
        return "S";
    }

    private String buildTaskReason(Task task, WorkspaceMember assignee, Map<UUID, MemberWorkload> loadMap) {
        StringBuilder sb = new StringBuilder();
        if (task.getPriority() == TaskPriority.URGENT) sb.append("URGENT priority; ");
        else if (task.getPriority() == TaskPriority.HIGH) sb.append("HIGH priority; ");
        if (task.isOverdue()) sb.append("overdue; ");
        if (task.getDueDate() != null) sb.append("due ").append(task.getDueDate()).append("; ");
        if (assignee != null && assignee.getUser() != null) {
            sb.append("assign to ").append(assignee.getUser().getFullName());
            MemberWorkload w = loadMap.get(assignee.getId());
            if (w != null && w.getWorkloadPercentage() != null) {
                sb.append(" (").append(w.getWorkloadPercentage()).append("% workload)");
            }
        } else {
            sb.append("no assignee found - needs assignment");
        }
        return sb.toString();
    }

    private TeamCapacity buildCapacity(WorkspaceSnapshot snapshot) {
        List<WorkspaceMember> members = snapshot.getMembers() != null ? snapshot.getMembers() : List.of();
        Map<UUID, MemberWorkload> loadMap = snapshot.getWorkloads() != null
                ? snapshot.getWorkloads().stream().collect(Collectors.toMap(w -> w.getMember().getId(), w -> w, (a, b) -> a))
                : Map.of();

        int available = 0, overloaded = 0;
        List<MemberCapacity> breakdown = new ArrayList<>();
        for (WorkspaceMember m : members) {
            if (m.getUser() == null) continue;
            int pct = 0;
            MemberWorkload w = loadMap.get(m.getId());
            if (w != null && w.getWorkloadPercentage() != null) pct = w.getWorkloadPercentage();
            if (pct < 70) available++;
            if (pct > 80) overloaded++;

            breakdown.add(MemberCapacity.builder()
                    .memberId(m.getId() != null ? m.getId().toString() : null)
                    .name(m.getUser().getFullName())
                    .role(m.getRole() != null ? m.getRole().getName() : null)
                    .currentWorkload(pct)
                    .canTakeMore(pct < 70 ? "yes" : pct < 85 ? "limited" : "no")
                    .build());
        }
        return TeamCapacity.builder()
                .totalMembers(members.size())
                .availableMembers(available)
                .overloadedMembers(overloaded)
                .breakdown(breakdown)
                .build();
    }

    private String buildSprintGoal(List<SprintTask> tasks, WorkspaceSnapshot snapshot) {
        long urgent = tasks.stream().filter(t -> "URGENT".equals(t.getPriority())).count();
        long high = tasks.stream().filter(t -> "HIGH".equals(t.getPriority())).count();
        long withAssignee = tasks.stream().filter(t -> t.getSuggestedAssigneeId() != null).count();
        return String.format(
                "Hoàn thành %d tasks (trong đó %d URGENT, %d HIGH) với %d/%d đã có assignee phù hợp.",
                tasks.size(), urgent, high, withAssignee, tasks.size());
    }

    private List<String> identifyRisks(List<SprintTask> tasks, WorkspaceSnapshot snapshot) {
        List<String> risks = new ArrayList<>();
        long unassigned = tasks.stream().filter(t -> t.getSuggestedAssigneeId() == null).count();
        if (unassigned > 0) {
            risks.add(unassigned + " tasks chưa có assignee - cần phân công trước sprint start.");
        }
        long overdue = tasks.stream().filter(t -> {
            if (t.getDueDate() == null) return false;
            return LocalDate.parse(t.getDueDate()).isBefore(snapshot.getToday());
        }).count();
        if (overdue > 0) {
            risks.add(overdue + " tasks đã quá hạn trong sprint plan - review lại feasibility.");
        }
        if (snapshot.getMembers() != null && snapshot.getWorkloads() != null) {
            Map<UUID, MemberWorkload> loadMap = snapshot.getWorkloads().stream()
                    .collect(Collectors.toMap(w -> w.getMember().getId(), w -> w, (a, b) -> a));
            long overloaded = snapshot.getMembers().stream()
                    .filter(m -> {
                        MemberWorkload w = loadMap.get(m.getId());
                        return w != null && w.getWorkloadPercentage() != null && w.getWorkloadPercentage() > 80;
                    }).count();
            if (overloaded > 0) {
                risks.add(overloaded + " thành viên đang overloaded - không nên nhận thêm task.");
            }
        }
        return risks;
    }

    private List<String> buildSuggestions(List<SprintTask> tasks, TeamCapacity capacity) {
        List<String> out = new ArrayList<>();
        out.add("Assign tất cả task chưa có assignee trước ngày bắt đầu sprint.");
        if (capacity.getAvailableMembers() > 0) {
            out.add("Có " + capacity.getAvailableMembers() + " thành viên có capacity - ưu tiên giao task cho họ.");
        }
        long highPri = tasks.stream()
                .filter(t -> "HIGH".equals(t.getPriority()) || "URGENT".equals(t.getPriority()))
                .count();
        out.add("Tập trung vào " + highPri + " task HIGH/URGENT trước.");
        out.add("Daily standup mỗi ngày để track progress và unblock sớm.");
        return out;
    }

    private int priorityRank(TaskPriority p) {
        return switch (p) {
            case URGENT -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
        };
    }
}
