package com.taskflow.ai.service;

import com.taskflow.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ProjectAnalysisPromptBuilder {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public String build(
            Project project,
            List<Task> tasks,
            List<MemberWorkload> workloads,
            List<Goal> goals,
            List<ActivityLog> activities) {

        StringBuilder prompt = new StringBuilder();

        prompt.append("You are a Senior Agile Project Manager analyzing a software project.\n\n");
        prompt.append(buildProjectSection(project)).append("\n\n");
        prompt.append(buildTaskSection(tasks)).append("\n\n");
        prompt.append(buildWorkloadSection(workloads)).append("\n\n");
        prompt.append(buildGoalSection(goals)).append("\n\n");
        prompt.append(buildActivitySection(activities)).append("\n\n");
        prompt.append(buildOutputSchema());

        return prompt.toString();
    }

    private String buildProjectSection(Project project) {
        StringBuilder sb = new StringBuilder();
        sb.append("PROJECT INFORMATION\n");
        sb.append("═══════════════════════════════════════\n");
        sb.append("Name: ").append(nullSafe(project.getName())).append("\n");
        sb.append("Key: ").append(nullSafe(project.getKey())).append("\n");
        if (project.getDescription() != null && !project.getDescription().isEmpty()) {
            sb.append("Description: ").append(project.getDescription()).append("\n");
        }
        sb.append("Created: ").append(
                project.getCreatedAt() != null ? project.getCreatedAt().format(DATE_FORMAT) : "N/A"
        ).append("\n");
        return sb.toString();
    }

    private String buildTaskSection(List<Task> tasks) {
        StringBuilder sb = new StringBuilder();
        sb.append("TASK STATISTICS\n");
        sb.append("═══════════════════════════════════════\n");

        if (tasks == null || tasks.isEmpty()) {
            sb.append("No tasks found.\n");
            return sb.toString();
        }

        int total = tasks.size();
        int completed = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        int inProgress = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        int todo = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.TODO).count();
        int overdue = (int) tasks.stream().filter(Task::isOverdue).count();
        int highPriority = (int) tasks.stream().filter(t -> t.getPriority() == TaskPriority.HIGH || t.getPriority() == TaskPriority.URGENT).count();

        sb.append("Total Tasks: ").append(total).append("\n");
        sb.append("Completed: ").append(completed).append("\n");
        sb.append("In Progress: ").append(inProgress).append("\n");
        sb.append("To Do: ").append(todo).append("\n");
        sb.append("Overdue: ").append(overdue).append("\n");
        sb.append("High/Critical Priority: ").append(highPriority).append("\n");
        sb.append("Completion Rate: ").append(total > 0 ? String.format("%.1f%%", (completed * 100.0 / total)) : "0%").append("\n\n");

        sb.append("RECENT COMPLETED TASKS:\n");
        tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .sorted((a, b) -> {
                    if (a.getUpdatedAt() == null || b.getUpdatedAt() == null) return 0;
                    return b.getUpdatedAt().compareTo(a.getUpdatedAt());
                })
                .limit(5)
                .forEach(t -> sb.append("  ✓ ").append(t.getTaskKey())
                        .append(": ").append(nullSafe(t.getTitle()))
                        .append("\n"));

        if (overdue > 0) {
            sb.append("\nOVERDUE TASKS:\n");
            tasks.stream()
                    .filter(Task::isOverdue)
                    .limit(5)
                    .forEach(t -> sb.append("  ⚠ ").append(t.getTaskKey())
                            .append(": ").append(nullSafe(t.getTitle()))
                            .append(" (due: ").append(t.getDueDate() != null ? t.getDueDate().format(DATE_FORMAT) : "N/A").append(")")
                            .append("\n"));
        }

        if (highPriority > 0) {
            sb.append("\nHIGH PRIORITY TASKS:\n");
            tasks.stream()
                    .filter(t -> t.getPriority() == TaskPriority.HIGH || t.getPriority() == TaskPriority.URGENT)
                    .filter(t -> t.getStatus() != TaskStatus.DONE)
                    .limit(5)
                    .forEach(t -> sb.append("  🔴 ").append(t.getTaskKey())
                            .append(": ").append(nullSafe(t.getTitle()))
                            .append(" [").append(t.getPriority()).append("]")
                            .append("\n"));
        }

        return sb.toString();
    }

    private String buildWorkloadSection(List<MemberWorkload> workloads) {
        StringBuilder sb = new StringBuilder();
        sb.append("TEAM WORKLOAD (Last 7 Days)\n");
        sb.append("═══════════════════════════════════════\n");

        if (workloads == null || workloads.isEmpty()) {
            sb.append("No workload data available.\n");
            return sb.toString();
        }

        double avgWorkload = workloads.stream()
                .mapToDouble(w -> w.getWorkloadPercentage() != null ? w.getWorkloadPercentage() : 0)
                .average()
                .orElse(0);

        sb.append("Average Team Workload: ").append(String.format("%.1f%%", avgWorkload)).append("\n\n");

        workloads.stream()
                .limit(10)
                .forEach(w -> {
                    String member = "Unknown";
                    if (w.getMember() != null && w.getMember().getUser() != null) {
                        member = w.getMember().getUser().getFullName();
                    }
                    double percentage = w.getWorkloadPercentage() != null ? w.getWorkloadPercentage() : 0;
                    int open = w.getOpenTasks() != null ? w.getOpenTasks() : 0;
                    int inProgress = w.getInProgressTasks() != null ? w.getInProgressTasks() : 0;

                    String status = percentage > 100 ? "🔴 Overloaded" :
                            percentage > 80 ? "🟡 High" :
                                    percentage > 50 ? "🟢 Normal" : "⚪ Low";

                    sb.append("  ").append(member)
                            .append(": ").append(String.format("%.0f%%", percentage))
                            .append(" (").append(open).append(" open, ").append(inProgress).append(" in progress)")
                            .append(" ").append(status)
                            .append("\n");
                });

        return sb.toString();
    }

    private String buildGoalSection(List<Goal> goals) {
        StringBuilder sb = new StringBuilder();
        sb.append("OKR/GOALS PROGRESS\n");
        sb.append("═══════════════════════════════════════\n");

        if (goals == null || goals.isEmpty()) {
            sb.append("No goals set for this project.\n");
            return sb.toString();
        }

        int totalGoals = goals.size();
        int activeGoals = (int) goals.stream().filter(g -> g.getStatus() == Goal.GoalStatus.ACTIVE).count();
        int achievedGoals = (int) goals.stream().filter(g -> g.getStatus() == Goal.GoalStatus.ACHIEVED).count();

        double avgProgress = goals.stream()
                .mapToInt(g -> g.getProgressPercentage() != null ? g.getProgressPercentage() : 0)
                .average()
                .orElse(0);

        sb.append("Total Goals: ").append(totalGoals).append("\n");
        sb.append("Active: ").append(activeGoals).append("\n");
        sb.append("Achieved: ").append(achievedGoals).append("\n");
        sb.append("Average Progress: ").append(String.format("%.1f%%", avgProgress)).append("\n\n");

        goals.stream()
                .limit(5)
                .forEach(g -> {
                    sb.append("  • ").append(nullSafe(g.getTitle()))
                            .append(" [").append(g.getStatus()).append("]")
                            .append(": ").append(g.getProgressPercentage()).append("%");
                    if (g.getDueDate() != null) {
                        sb.append(" (due: ").append(g.getDueDate().format(DATE_FORMAT)).append(")");
                    }
                    sb.append("\n");
                });

        return sb.toString();
    }

    private String buildActivitySection(List<ActivityLog> activities) {
        StringBuilder sb = new StringBuilder();
        sb.append("RECENT ACTIVITY\n");
        sb.append("═══════════════════════════════════════\n");

        if (activities == null || activities.isEmpty()) {
            sb.append("No recent activity.\n");
            return sb.toString();
        }

        activities.stream()
                .limit(10)
                .forEach(a -> {
                    String user = a.getUser() != null ? a.getUser().getFullName() : "System";
                    sb.append("  • ").append(nullSafe(user))
                            .append(" ").append(nullSafe(a.getAction()))
                            .append(" ").append(nullSafe(a.getEntityType()))
                            .append(" at ").append(a.getCreatedAt() != null ? a.getCreatedAt().format(DATE_FORMAT) : "N/A")
                            .append("\n");
                });

        return sb.toString();
    }

    private String buildOutputSchema() {
        return """
                ANALYSIS REQUEST
                ════════════════════════════════════════
                Based on the data above, provide a project health analysis as a Senior Agile Project Manager.

                IMPORTANT: Return STRICTLY valid JSON with this exact structure:
                {
                  "healthScore": 0-100,
                  "summary": "2-3 sentence executive summary of project health",
                  "risks": ["EXACTLY 3 risks - most critical first"],
                  "recommendations": ["EXACTLY 3 actionable suggestions"],
                  "nextActions": ["EXACTLY 3 immediate next steps"],
                  "confidence": 0.0-1.0
                }

                CRITICAL RULES:
                - Return ONLY JSON, NO markdown (no ```json, no ```, no explanation)
                - healthScore: integer 0-100 (0=critical, 100=excellent)
                - risks: EXACTLY 3 items, most critical first, max 150 characters each
                - recommendations: EXACTLY 3 items, actionable and specific, max 150 characters each
                - nextActions: EXACTLY 3 items, immediate actionable steps, max 150 characters each
                - confidence: decimal 0.0-1.0
                """;
    }

    private String nullSafe(String value) {
        return value != null ? value : "N/A";
    }
}
