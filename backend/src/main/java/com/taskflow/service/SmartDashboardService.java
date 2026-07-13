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
        response.setTeamPerformance(getTeamPerformance(workspaceId, workspaceTasks));
        response.setStats(getStats(workspaceId, workspaceTasks));

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
}