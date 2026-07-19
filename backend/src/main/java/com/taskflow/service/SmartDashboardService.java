package com.taskflow.service;

import com.taskflow.dto.response.SmartDashboardResponse;
import com.taskflow.dto.response.SmartDashboardResponse.*;
import com.taskflow.entity.*;
import com.taskflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SmartDashboardService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ActivityLogRepository activityLogRepository;

    public SmartDashboardResponse getSmartDashboard(UUID workspaceId, UUID userId) {
        long startTime = System.currentTimeMillis();
        SmartDashboardResponse response = new SmartDashboardResponse();

        // Load the workspace task set ONCE; everything else filters from this list
        // instead of running findAll() repeatedly.
        List<Task> workspaceTasks = taskRepository.findAllByWorkspaceIdEager(workspaceId);

        response.setMyTasks(getMyTasks(workspaceTasks, userId));
        response.setOverdueTasks(getOverdueTasks(workspaceId, userId));
        response.setUpcomingDeadlines(getUpcomingDeadlines(workspaceId));
        response.setRecentActivity(getRecentActivity(workspaceId));
        response.setWorkloadChart(getWorkloadChart(workspaceId, workspaceTasks));
        response.setAiSuggestions(generateAiSuggestions(workspaceId, userId, workspaceTasks));
        response.setRecommendedActions(generateRecommendedActions(workspaceId, userId, workspaceTasks));
        response.setTeamPerformance(getTeamPerformance(workspaceId, workspaceTasks));
        response.setStats(getStats(workspaceId, workspaceTasks));
        response.setRiskScore(computeRiskScore(workspaceTasks));
        response.setProjectHealth(computeProjectHealth(workspaceId, workspaceTasks));
        response.setDelayedTasks(getDelayedTasks(workspaceTasks));
        response.setLastUpdatedAt(java.time.LocalDateTime.now().toString());
        response.setProcessingTimeMs(System.currentTimeMillis() - startTime);

        return response;
    }

    private MyTasks getMyTasks(List<Task> workspaceTasks, UUID userId) {
        List<Task> allTasks = workspaceTasks.stream()
                .filter(t -> t.getAssignee() != null && t.getAssignee().getId().equals(userId))
                .collect(Collectors.toList());

        List<TaskResponse> todo = allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.TODO)
                .map(this::mapToTaskResponse)
                .collect(Collectors.toList());

        List<TaskResponse> inProgress = allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS)
                .map(this::mapToTaskResponse)
                .collect(Collectors.toList());

        List<TaskResponse> review = allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.REVIEW)
                .map(this::mapToTaskResponse)
                .collect(Collectors.toList());

        return MyTasks.builder()
                .todo(todo)
                .inProgress(inProgress)
                .review(review)
                .totalCount(allTasks.size())
                .build();
    }

    private List<TaskResponse> getOverdueTasks(UUID workspaceId, UUID userId) {
        LocalDate today = LocalDate.now();

        return taskRepository.findOverdueByWorkspaceAndAssignee(workspaceId, userId, today, TaskStatus.DONE).stream()
                .map(this::mapToTaskResponse)
                .collect(Collectors.toList());
    }

    private List<UpcomingDeadline> getUpcomingDeadlines(UUID workspaceId) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(7);

        return taskRepository.findUpcomingByWorkspace(workspaceId, today, endDate, TaskStatus.DONE).stream()
                .limit(10)
                .map(t -> UpcomingDeadline.builder()
                        .taskId(t.getId().toString())
                        .taskKey(t.getProject().getKey() + "-" + t.getTaskNumber())
                        .title(t.getTitle())
                        .dueDate(t.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                        .daysLeft((int) ChronoUnit.DAYS.between(today, t.getDueDate()))
                        .priority(t.getPriority() != null ? t.getPriority().name() : null)
                        .build())
                .collect(Collectors.toList());
    }

    private List<ActivityItem> getRecentActivity(UUID workspaceId) {
        return activityLogRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId).stream()
                .limit(10)
                .map(log -> ActivityItem.builder()
                        .userId(log.getUser().getId().toString())
                        .userName(log.getUser().getFullName())
                        .userAvatar(log.getUser().getAvatarUrl())
                        .action(log.getAction())
                        .entityType(log.getEntityType())
                        .entityId(log.getEntityId().toString())
                        .description(formatActivityDescription(log))
                        .timestamp(log.getCreatedAt() != null ? log.getCreatedAt().toString() : null)
                        .build())
                .collect(Collectors.toList());
    }

    private String formatActivityDescription(ActivityLog log) {
        String userName = log.getUser().getFullName();
        String action = log.getAction().toLowerCase();
        String entity = log.getEntityType().toLowerCase();

        return switch (log.getAction()) {
            case "CREATED" -> String.format("%s created %s", userName, entity);
            case "UPDATED" -> String.format("%s updated %s", userName, entity);
            case "DELETED" -> String.format("%s deleted %s", userName, entity);
            case "INVITED" -> String.format("%s invited member", userName);
            default -> String.format("%s performed %s on %s", userName, action, entity);
        };
    }

    private WorkloadChart getWorkloadChart(UUID workspaceId, List<Task> workspaceTasks) {
        List<WorkspaceMember> members = workspaceMemberRepository.findAllByWorkspaceId(workspaceId);
        List<MemberLoad> workloads = new ArrayList<>();
        int totalOpenTasks = 0;

        // Build a quick assignee_id -> tasks index so we don't loop the workspace
        // task list once per member.
        Map<UUID, List<Task>> tasksByAssignee = workspaceTasks.stream()
                .filter(t -> t.getAssignee() != null)
                .collect(Collectors.groupingBy(t -> t.getAssignee().getId()));

        for (WorkspaceMember member : members) {
            UUID memberId = member.getUser().getId();

            List<Task> memberTasks = tasksByAssignee.getOrDefault(memberId, Collections.emptyList());

            int openTasks = (int) memberTasks.stream()
                    .filter(t -> t.getStatus() != TaskStatus.DONE)
                    .count();

            int completedTasks = (int) memberTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.DONE)
                    .count();

            totalOpenTasks += openTasks;

            String status;
            if (openTasks == 0) status = "UNDERUTILIZED";
            else if (openTasks <= 5) status = "BALANCED";
            else status = "OVERLOADED";

            workloads.add(MemberLoad.builder()
                    .memberId(memberId.toString())
                    .memberName(member.getUser().getFullName())
                    .avatarUrl(member.getUser().getAvatarUrl())
                    .openTasks(openTasks)
                    .completedTasks(completedTasks)
                    .loadPercentage(0)
                    .status(status)
                    .build());
        }

        final int total = totalOpenTasks;
        workloads.forEach(w -> {
            if (total > 0) {
                w.setLoadPercentage(Math.round((double) w.getOpenTasks() / total * 100));
            }
        });

        double avgLoad = workloads.isEmpty() ? 0 :
                workloads.stream().mapToInt(MemberLoad::getOpenTasks).average().orElse(0);

        return WorkloadChart.builder()
                .members(workloads)
                .averageLoad(avgLoad)
                .build();
    }

    private List<AiSuggestion> generateAiSuggestions(UUID workspaceId, UUID userId, List<Task> workspaceTasks) {
        List<AiSuggestion> suggestions = new ArrayList<>();

        WorkloadChart workload = getWorkloadChart(workspaceId, workspaceTasks);
        for (MemberLoad member : workload.getMembers()) {
            if ("OVERLOADED".equals(member.getStatus())) {
                suggestions.add(AiSuggestion.builder()
                        .id(UUID.randomUUID().toString())
                        .type("REASSIGN")
                        .title("Workload Imbalance Detected")
                        .description(String.format("%s has %d open tasks. Consider redistributing workload.",
                                member.getMemberName(), member.getOpenTasks()))
                        .priority("HIGH")
                        .actionLabel("View Team")
                        .metadata(Map.of("memberId", member.getMemberId()))
                        .build());
            }
        }

        List<TaskResponse> overdue = getOverdueTasks(workspaceId, userId);
        if (overdue.size() >= 3) {
            suggestions.add(AiSuggestion.builder()
                    .id(UUID.randomUUID().toString())
                    .type("DEADLINE")
                    .title("Multiple Overdue Tasks")
                    .description(String.format("You have %d overdue tasks. Consider rescheduling or reassigning.", overdue.size()))
                    .priority("HIGH")
                    .actionLabel("View Tasks")
                    .build());
        }

        long unassignedCount = workspaceTasks.stream()
                .filter(t -> t.getAssignee() == null)
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .count();

        if (unassignedCount >= 5) {
            suggestions.add(AiSuggestion.builder()
                    .id(UUID.randomUUID().toString())
                    .type("REASSIGN")
                    .title("Unassigned Tasks")
                    .description(String.format("There are %d tasks without assignees. Assign them to team members.", unassignedCount))
                    .priority("MEDIUM")
                    .actionLabel("View Tasks")
                    .build());
        }

        LocalDate today = LocalDate.now();
        LocalDate endOfWeek = today.plusDays(7 - today.getDayOfWeek().getValue());

        long upcomingDeadlines = workspaceTasks.stream()
                .filter(t -> t.getDueDate() != null)
                .filter(t -> {
                    LocalDate dueDate = t.getDueDate();
                    return !dueDate.isBefore(today) && !dueDate.isAfter(endOfWeek);
                })
                .count();

        if (upcomingDeadlines > 10) {
            suggestions.add(AiSuggestion.builder()
                    .id(UUID.randomUUID().toString())
                    .type("DEADLINE")
                    .title("Busy Week Ahead")
                    .description(String.format("%d tasks are due this week. Plan your week accordingly.", upcomingDeadlines))
                    .priority("MEDIUM")
                    .actionLabel("View Calendar")
                    .build());
        }

        return suggestions;
    }

    private TeamPerformance getTeamPerformance(UUID workspaceId, List<Task> workspaceTasks) {
        int totalTasks = workspaceTasks.size();
        int completedTasks = (int) workspaceTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .count();

        double completionRate = totalTasks > 0 ? (double) completedTasks / totalTasks * 100 : 0;

        int activeMembers = workspaceMemberRepository.findAllByWorkspaceId(workspaceId).size();
        double avgTasksPerMember = activeMembers > 0 ? (double) totalTasks / activeMembers : 0;

        LocalDate today = LocalDate.now();
        List<DailyActivity> dailyActivity = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            final LocalDate targetDate = date;

            int created = (int) workspaceTasks.stream()
                    .filter(t -> t.getCreatedAt() != null &&
                               t.getCreatedAt().toLocalDate().equals(targetDate))
                    .count();

            int completed = (int) workspaceTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.DONE &&
                               t.getUpdatedAt() != null &&
                               t.getUpdatedAt().toLocalDate().equals(targetDate))
                    .count();

            dailyActivity.add(DailyActivity.builder()
                    .date(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .tasksCreated(created)
                    .tasksCompleted(completed)
                    .build());
        }

        return TeamPerformance.builder()
                .completionRate(Math.round(completionRate * 100) / 100.0)
                .averageTasksPerMember(Math.round(avgTasksPerMember * 100) / 100.0)
                .productivityScore(Math.round(completionRate * 0.7 + (100 - completionRate) * 0.3))
                .activeMembers(activeMembers)
                .newTasksThisWeek(dailyActivity.stream().mapToInt(DailyActivity::getTasksCreated).sum())
                .completedTasksThisWeek(dailyActivity.stream().mapToInt(DailyActivity::getTasksCompleted).sum())
                .dailyActivity(dailyActivity)
                .build();
    }

    private DashboardStats getStats(UUID workspaceId, List<Task> workspaceTasks) {
        int totalTasks = workspaceTasks.size();
        int completedTasks = (int) workspaceTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .count();
        int inProgressTasks = (int) workspaceTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS)
                .count();
        int todoTasks = (int) workspaceTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.TODO)
                .count();

        LocalDate today = LocalDate.now();
        int overdueCount = (int) workspaceTasks.stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(today))
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .count();

        Map<String, Integer> tasksByStatus = new HashMap<>();
        tasksByStatus.put("TODO", todoTasks);
        tasksByStatus.put("IN_PROGRESS", inProgressTasks);
        tasksByStatus.put("REVIEW", (int) workspaceTasks.stream().filter(t -> t.getStatus() == TaskStatus.REVIEW).count());
        tasksByStatus.put("DONE", completedTasks);

        Map<String, Integer> tasksByPriority = new HashMap<>();
        for (TaskPriority priority : TaskPriority.values()) {
            tasksByPriority.put(priority.name(),
                    (int) workspaceTasks.stream().filter(t -> t.getPriority() == priority).count());
        }

        return DashboardStats.builder()
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .inProgressTasks(inProgressTasks)
                .todoTasks(todoTasks)
                .delayedTasks(overdueCount)
                .completionRate(totalTasks > 0 ? Math.round((double) completedTasks / totalTasks * 100 * 100) / 100.0 : 0)
                .overdueCount(overdueCount)
                .tasksByPriority(tasksByPriority)
                .tasksByStatus(tasksByStatus)
                .build();
    }

    private TaskResponse mapToTaskResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId().toString())
                .title(task.getTitle())
                .status(task.getStatus() != null ? task.getStatus().name() : null)
                .priority(task.getPriority() != null ? task.getPriority().name() : null)
                .projectName(task.getProject() != null ? task.getProject().getName() : null)
                .projectKey(task.getProject() != null ? task.getProject().getKey() : null)
                .taskKey((task.getProject() != null ? task.getProject().getKey() : "") + "-" + task.getTaskNumber())
                .dueDate(task.getDueDate() != null ? task.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null)
                .assigneeName(task.getAssignee() != null ? task.getAssignee().getFullName() : null)
                .assigneeAvatar(task.getAssignee() != null ? task.getAssignee().getAvatarUrl() : null)
                .overdue(task.getDueDate() != null &&
                        task.getDueDate().isBefore(LocalDate.now()) &&
                        task.getStatus() != TaskStatus.DONE)
                .build();
    }

    // ---------------------------------------------------------------------
    // Risk Score
    // ---------------------------------------------------------------------

    private RiskScore computeRiskScore(List<Task> workspaceTasks) {
        List<RiskFactor> factors = new ArrayList<>();

        LocalDate today = LocalDate.now();
        long total = workspaceTasks.size();
        long overdue = workspaceTasks.stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(today))
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .count();

        long unassigned = workspaceTasks.stream()
                .filter(t -> t.getAssignee() == null && t.getStatus() != TaskStatus.DONE)
                .count();

        long nearDeadline = workspaceTasks.stream()
                .filter(t -> t.getDueDate() != null)
                .filter(t -> !t.getDueDate().isBefore(today))
                .filter(t -> t.getDueDate().isBefore(today.plusDays(3)))
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .count();

        long overdueHighPriority = workspaceTasks.stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(today))
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .filter(t -> t.getPriority() == TaskPriority.HIGH || t.getPriority() == TaskPriority.URGENT)
                .count();

        // Weight contributions out of 100.
        int overdueWeight = clamp((int) Math.round((double) overdue / Math.max(1, total) * 250), 0, 40);
        int unassignedWeight = clamp((int) Math.round((double) unassigned / Math.max(1, total) * 200), 0, 20);
        int nearDeadlineWeight = clamp((int) Math.round((double) nearDeadline / Math.max(1, total) * 150), 0, 20);
        int overdueHighWeight = clamp((int) overdueHighPriority * 5, 0, 20);

        int score = Math.min(100, overdueWeight + unassignedWeight + nearDeadlineWeight + overdueHighWeight);
        String level = scoreToLevel(score);

        if (overdue > 0) {
            factors.add(RiskFactor.builder()
                    .code("OVERDUE")
                    .label("Overdue tasks")
                    .weight(overdueWeight)
                    .level(overdue > 5 ? "HIGH" : overdue > 2 ? "MEDIUM" : "LOW")
                    .count((int) overdue)
                    .recommendation("Reschedule or escalate overdue tasks immediately.")
                    .build());
        }
        if (unassigned > 0) {
            factors.add(RiskFactor.builder()
                    .code("UNASSIGNED")
                    .label("Unassigned tasks")
                    .weight(unassignedWeight)
                    .level(unassigned > 5 ? "HIGH" : "MEDIUM")
                    .count((int) unassigned)
                    .recommendation("Assign tasks to team members using AI recommendation.")
                    .build());
        }
        if (nearDeadline > 0) {
            factors.add(RiskFactor.builder()
                    .code("NEAR_DEADLINE")
                    .label("Tasks due within 3 days")
                    .weight(nearDeadlineWeight)
                    .level(nearDeadline > 5 ? "HIGH" : "MEDIUM")
                    .count((int) nearDeadline)
                    .recommendation("Prioritize upcoming deadlines.")
                    .build());
        }
        if (overdueHighPriority > 0) {
            factors.add(RiskFactor.builder()
                    .code("OVERDUE_HIGH_PRIORITY")
                    .label("Overdue high-priority tasks")
                    .weight(overdueHighWeight)
                    .level("HIGH")
                    .count((int) overdueHighPriority)
                    .recommendation("Escalate high-priority overdue items to project manager.")
                    .build());
        }

        String summary;
        if (score >= 75) summary = "Workspace is at critical risk. Immediate action required.";
        else if (score >= 50) summary = "Workspace risk is elevated. Several issues need attention.";
        else if (score >= 25) summary = "Workspace is moderately healthy with some concerns.";
        else summary = "Workspace is healthy. Keep up the momentum.";

        return RiskScore.builder()
                .score(score)
                .level(level)
                .factors(factors)
                .summary(summary)
                .build();
    }

    private String scoreToLevel(int score) {
        if (score >= 75) return "CRITICAL";
        if (score >= 50) return "HIGH";
        if (score >= 25) return "MEDIUM";
        return "LOW";
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    // ---------------------------------------------------------------------
    // Project Health (per-project breakdown)
    // ---------------------------------------------------------------------

    private ProjectHealth computeProjectHealth(UUID workspaceId, List<Task> workspaceTasks) {
        List<Project> projects = projectRepository.findAllByWorkspaceId(workspaceId);

        Map<UUID, List<Task>> tasksByProject = workspaceTasks.stream()
                .filter(t -> t.getProject() != null)
                .collect(Collectors.groupingBy(t -> t.getProject().getId()));

        LocalDate today = LocalDate.now();
        List<ProjectHealthItem> items = new ArrayList<>();
        int excellent = 0, good = 0, atRisk = 0, critical = 0;
        int totalScore = 0;
        int scoredProjects = 0;

        for (Project p : projects) {
            List<Task> projectTasks = tasksByProject.getOrDefault(p.getId(), Collections.emptyList());
            int total = projectTasks.size();
            int completed = (int) projectTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.DONE).count();
            int overdue = (int) projectTasks.stream()
                    .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(today))
                    .filter(t -> t.getStatus() != TaskStatus.DONE).count();
            int unassigned = (int) projectTasks.stream()
                    .filter(t -> t.getAssignee() == null && t.getStatus() != TaskStatus.DONE).count();

            double completionRate = total > 0 ? Math.round((double) completed / total * 100 * 100) / 100.0 : 0;

            // Per-project health: start at 100, subtract penalties.
            int health = 100;
            if (total > 0) {
                health -= (int) Math.round((double) overdue / total * 100);
                health -= (int) Math.round((double) unassigned / total * 60);
                if (completionRate < 20 && total > 5) health -= 10;
            } else {
                health = 100; // empty project = healthy
            }
            health = clamp(health, 0, 100);

            String healthLevel;
            if (health >= 85) { healthLevel = "EXCELLENT"; excellent++; }
            else if (health >= 65) { healthLevel = "GOOD"; good++; }
            else if (health >= 40) { healthLevel = "AT_RISK"; atRisk++; }
            else { healthLevel = "CRITICAL"; critical++; }

            List<String> issues = new ArrayList<>();
            if (overdue > 0) issues.add(overdue + " overdue task(s)");
            if (unassigned > 0) issues.add(unassigned + " unassigned task(s)");
            if (completionRate < 25 && total > 0) issues.add("low completion (" + completionRate + "%)");

            totalScore += health;
            scoredProjects++;

            items.add(ProjectHealthItem.builder()
                    .projectId(p.getId().toString())
                    .projectName(p.getName())
                    .projectKey(p.getKey())
                    .healthLevel(healthLevel)
                    .healthScore(health)
                    .totalTasks(total)
                    .overdueTasks(overdue)
                    .unassignedTasks(unassigned)
                    .completionRate(completionRate)
                    .trend("STABLE")
                    .issues(issues)
                    .build());
        }

        // Sort by health ascending so the most problematic projects surface first.
        items.sort(Comparator.comparingInt(ProjectHealthItem::getHealthScore));

        double workspaceHealth = scoredProjects > 0
                ? Math.round((double) totalScore / scoredProjects * 100) / 100.0
                : 100.0;

        return ProjectHealth.builder()
                .projects(items)
                .summary(ProjectHealthSummary.builder()
                        .excellent(excellent)
                        .good(good)
                        .atRisk(atRisk)
                        .critical(critical)
                        .workspaceHealthScore(workspaceHealth)
                        .build())
                .build();
    }

    // ---------------------------------------------------------------------
    // Delayed tasks
    // ---------------------------------------------------------------------

    private List<DelayedTask> getDelayedTasks(List<Task> workspaceTasks) {
        LocalDate today = LocalDate.now();
        return workspaceTasks.stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(today))
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .sorted(Comparator.comparing(Task::getDueDate))
                .limit(20)
                .map(t -> DelayedTask.builder()
                        .taskId(t.getId().toString())
                        .taskKey((t.getProject() != null ? t.getProject().getKey() : "") + "-" + t.getTaskNumber())
                        .title(t.getTitle())
                        .projectName(t.getProject() != null ? t.getProject().getName() : null)
                        .assigneeName(t.getAssignee() != null ? t.getAssignee().getFullName() : null)
                        .assigneeAvatar(t.getAssignee() != null ? t.getAssignee().getAvatarUrl() : null)
                        .dueDate(t.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                        .daysOverdue((int) ChronoUnit.DAYS.between(t.getDueDate(), today))
                        .priority(t.getPriority() != null ? t.getPriority().name() : null)
                        .impact(daysOverdueImpact(t))
                        .build())
                .collect(Collectors.toList());
    }

    private String daysOverdueImpact(Task t) {
        long days = ChronoUnit.DAYS.between(t.getDueDate(), LocalDate.now());
        if (t.getPriority() == TaskPriority.URGENT || days > 7) return "HIGH";
        if (t.getPriority() == TaskPriority.HIGH || days > 3) return "MEDIUM";
        return "LOW";
    }

    // ---------------------------------------------------------------------
    // Recommended Actions (concrete next steps)
    // ---------------------------------------------------------------------

    private List<RecommendedAction> generateRecommendedActions(UUID workspaceId, UUID userId,
                                                                List<Task> workspaceTasks) {
        List<RecommendedAction> actions = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 1. Highest priority: overdue critical/high tasks -> escalate.
        List<Task> overdueCritical = workspaceTasks.stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(today))
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .filter(t -> t.getPriority() == TaskPriority.URGENT || t.getPriority() == TaskPriority.HIGH)
                .sorted(Comparator.comparing(Task::getDueDate))
                .limit(3)
                .toList();

        if (!overdueCritical.isEmpty()) {
            Task first = overdueCritical.get(0);
            String projectId = first.getProject() != null ? first.getProject().getId().toString() : "";
            actions.add(RecommendedAction.builder()
                    .id(UUID.randomUUID().toString())
                    .type("ESCALATE")
                    .title("Escalate " + overdueCritical.size() + " overdue high-priority task(s)")
                    .description("These tasks are overdue and need immediate attention or replanning.")
                    .priority("HIGH")
                    .actionLabel("Review Overdue")
                    .actionEndpoint("/api/dashboard/smart?workspaceId=" + workspaceId)
                    .actionMethod("GET")
                    .metadata(Map.of("count", String.valueOf(overdueCritical.size()),
                            "projectId", projectId))
                    .build());
        }

        // 2. Overloaded members -> reassign.
        WorkloadChart workload = getWorkloadChart(workspaceId, workspaceTasks);
        long overloaded = workload.getMembers().stream()
                .filter(m -> "OVERLOADED".equals(m.getStatus())).count();
        if (overloaded > 0) {
            actions.add(RecommendedAction.builder()
                    .id(UUID.randomUUID().toString())
                    .type("REASSIGN")
                    .title("Rebalance " + overloaded + " overloaded team member(s)")
                    .description("Run AI task assignment to redistribute work from overloaded members.")
                    .priority("HIGH")
                    .actionLabel("Run AI Reassignment")
                    .actionEndpoint("/api/ai/tasks/assign-batch")
                    .actionMethod("POST")
                    .build());
        }

        // 3. Unassigned tasks -> assign.
        long unassigned = workspaceTasks.stream()
                .filter(t -> t.getAssignee() == null && t.getStatus() != TaskStatus.DONE).count();
        if (unassigned > 0) {
            actions.add(RecommendedAction.builder()
                    .id(UUID.randomUUID().toString())
                    .type("REASSIGN")
                    .title("Assign " + unassigned + " unassigned task(s)")
                    .description("Use AI to assign these tasks based on role and workload.")
                    .priority("MEDIUM")
                    .actionLabel("Assign Now")
                    .actionEndpoint("/api/ai/tasks/assign-batch")
                    .actionMethod("POST")
                    .build());
        }

        // 4. Near-deadline upcoming -> review.
        long nearDeadline = workspaceTasks.stream()
                .filter(t -> t.getDueDate() != null)
                .filter(t -> !t.getDueDate().isBefore(today))
                .filter(t -> t.getDueDate().isBefore(today.plusDays(3)))
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .count();
        if (nearDeadline >= 3) {
            actions.add(RecommendedAction.builder()
                    .id(UUID.randomUUID().toString())
                    .type("REVIEW")
                    .title(nearDeadline + " tasks due in the next 3 days")
                    .description("Review upcoming deadlines and ensure they are on track.")
                    .priority("MEDIUM")
                    .actionLabel("View Calendar")
                    .actionEndpoint("/api/dashboard/smart?workspaceId=" + workspaceId)
                    .actionMethod("GET")
                    .build());
        }

        // 5. Many TODO tasks -> split or prioritize.
        long todoTasks = workspaceTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.TODO).count();
        if (todoTasks > workspaceTasks.size() * 0.6 && workspaceTasks.size() > 10) {
            actions.add(RecommendedAction.builder()
                    .id(UUID.randomUUID().toString())
                    .type("PRIORITIZE")
                    .title("Backlog is large - prioritize " + todoTasks + " TODO tasks")
                    .description("Many tasks are in TODO. Review priorities and split large tasks into subtasks.")
                    .priority("LOW")
                    .actionLabel("View Backlog")
                    .actionEndpoint("/api/dashboard/smart?workspaceId=" + workspaceId)
                    .actionMethod("GET")
                    .build());
        }

        // 6. If overall workspace health is good, surface a "celebrate" action.
        if (actions.isEmpty()) {
            actions.add(RecommendedAction.builder()
                    .id(UUID.randomUUID().toString())
                    .type("REVIEW")
                    .title("Workspace is healthy")
                    .description("All key metrics look good. Consider scheduling a retrospective or planning next sprint.")
                    .priority("LOW")
                    .actionLabel("View Dashboard")
                    .actionEndpoint("/api/dashboard/smart?workspaceId=" + workspaceId)
                    .actionMethod("GET")
                    .build());
        }

        return actions;
    }
}