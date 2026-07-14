package com.taskflow.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartDashboardResponse {
    
    private MyTasks myTasks;
    private List<TaskResponse> overdueTasks;
    private List<UpcomingDeadline> upcomingDeadlines;
    private List<ActivityItem> recentActivity;
    private WorkloadChart workloadChart;
    private List<AiSuggestion> aiSuggestions;
    private List<RecommendedAction> recommendedActions;
    private TeamPerformance teamPerformance;
    private DashboardStats stats;
    private RiskScore riskScore;
    private ProjectHealth projectHealth;
    private List<DelayedTask> delayedTasks;
    private String lastUpdatedAt;
    private Long processingTimeMs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MyTasks {
        private List<TaskResponse> todo;
        private List<TaskResponse> inProgress;
        private List<TaskResponse> review;
        private int totalCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpcomingDeadline {
        private String taskId;
        private String taskKey;
        private String title;
        private String dueDate;
        private int daysLeft;
        private String priority;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActivityItem {
        private String userId;
        private String userName;
        private String userAvatar;
        private String action;
        private String entityType;
        private String entityId;
        private String description;
        private String timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WorkloadChart {
        private List<MemberLoad> members;
        private double averageLoad;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MemberLoad {
        private String memberId;
        private String memberName;
        private String avatarUrl;
        private int openTasks;
        private int completedTasks;
        private double loadPercentage;
        private String status; // UNDERUTILIZED, BALANCED, OVERLOADED
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AiSuggestion {
        private String id;
        private String type; // REASSIGN, REDESIGN, SPLIT_TASK, DEADLINE, PRIORITIZE
        private String title;
        private String description;
        private String priority; // HIGH, MEDIUM, LOW
        private String actionLabel;
        private Map<String, String> metadata;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TeamPerformance {
        private double completionRate;
        private double averageTasksPerMember;
        private double productivityScore;
        private int activeMembers;
        private int newTasksThisWeek;
        private int completedTasksThisWeek;
        private List<DailyActivity> dailyActivity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyActivity {
        private String date;
        private int tasksCreated;
        private int tasksCompleted;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DashboardStats {
        private int totalTasks;
        private int completedTasks;
        private int inProgressTasks;
        private int todoTasks;
        private int delayedTasks;
        private double completionRate;
        private int overdueCount;
        private Map<String, Integer> tasksByPriority;
        private Map<String, Integer> tasksByStatus;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TaskResponse {
        private String id;
        private String title;
        private String status;
        private String priority;
        private String projectName;
        private String projectKey;
        private String taskKey;
        private String dueDate;
        private String assigneeName;
        private String assigneeAvatar;
        private boolean overdue;
    }

    /**
     * Aggregated risk score 0-100. 100 = worst case, 0 = healthy.
     * Computed from overdue ratio, blocking dependencies, workload imbalance, etc.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RiskScore {
        private int score;            // 0-100
        private String level;         // LOW / MEDIUM / HIGH / CRITICAL
        private List<RiskFactor> factors;
        private String summary;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RiskFactor {
        private String code;          // OVERDUE, OVERLOAD, UNASSIGNED, NEAR_DEADLINE, DEPENDENCY
        private String label;
        private int weight;           // contribution to total score (0-100)
        private String level;         // LOW / MEDIUM / HIGH
        private int count;            // how many instances of this factor
        private String recommendation;
    }

    /**
     * Project-level health breakdown. Useful for multi-project dashboards.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProjectHealth {
        private List<ProjectHealthItem> projects;
        private ProjectHealthSummary summary;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProjectHealthItem {
        private String projectId;
        private String projectName;
        private String projectKey;
        private String healthLevel;     // EXCELLENT / GOOD / AT_RISK / CRITICAL
        private int healthScore;        // 0-100
        private int totalTasks;
        private int overdueTasks;
        private int unassignedTasks;
        private double completionRate;  // 0-100
        private String trend;           // IMPROVING / STABLE / DECLINING
        private List<String> issues;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProjectHealthSummary {
        private int excellent;
        private int good;
        private int atRisk;
        private int critical;
        private double workspaceHealthScore;
    }

    /**
     * Delayed (overdue) task with impact analysis.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DelayedTask {
        private String taskId;
        private String taskKey;
        private String title;
        private String projectName;
        private String assigneeName;
        private String assigneeAvatar;
        private String dueDate;
        private int daysOverdue;
        private String priority;
        private String impact;          // LOW / MEDIUM / HIGH
    }

    /**
     * Concrete actionable recommendation. Unlike AiSuggestion (which is more
     * observational), RecommendedAction is something the user can click and
     * resolve directly.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendedAction {
        private String id;
        private String type;            // REASSIGN, RESCHEDULE, SPLIT, REVIEW, ESCALATE, CLEANUP
        private String title;
        private String description;
        private String priority;        // HIGH / MEDIUM / LOW
        private String actionLabel;
        private String actionEndpoint;  // relative API path the frontend can call
        private String actionMethod;    // GET / POST / PUT / DELETE
        private Map<String, String> metadata;
    }
}
