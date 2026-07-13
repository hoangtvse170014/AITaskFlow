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
    private TeamPerformance teamPerformance;
    private DashboardStats stats;

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
}
