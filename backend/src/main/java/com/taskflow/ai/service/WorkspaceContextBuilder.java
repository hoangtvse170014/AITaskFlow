package com.taskflow.ai.service;

import com.taskflow.entity.*;
import com.taskflow.entity.TaskStatus;
import com.taskflow.entity.TaskPriority;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class WorkspaceContextBuilder {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public String buildContext(
            Workspace workspace,
            List<Project> projects,
            List<Task> tasks,
            List<Goal> goals,
            List<Page> pages,
            List<WorkspaceMember> members,
            List<MemberWorkload> workloads,
            List<ActivityLog> activities,
            String question) {

        StringBuilder context = new StringBuilder();

        context.append("═══════════════════════════════════════════════════════════════════\n");
        context.append("WORKSPACE AI ASSISTANT CONTEXT\n");
        context.append("═══════════════════════════════════════════════════════════════════\n\n");

        context.append(buildWorkspaceSection(workspace));
        context.append("\n");

        context.append(buildProjectsSection(projects, tasks));
        context.append("\n");

        context.append(buildTasksSection(tasks));
        context.append("\n");

        context.append(buildGoalsSection(goals));
        context.append("\n");

        context.append(buildMembersSection(members, workloads));
        context.append("\n");

        context.append(buildPagesSection(pages));
        context.append("\n");

        context.append(buildRecentActivitySection(activities));
        context.append("\n");

        context.append(buildQuestionSection(question));
        context.append("\n");

        context.append(buildOutputInstructions());

        return context.toString();
    }

    private String buildWorkspaceSection(Workspace workspace) {
        StringBuilder sb = new StringBuilder();
        sb.append("WORKSPACE INFORMATION\n");
        sb.append("───────────────────────────────────────────────────────────────────\n");
        sb.append("Name: ").append(nullSafe(workspace.getName())).append("\n");
        sb.append("Slug: ").append(nullSafe(workspace.getSlug())).append("\n");
        if (workspace.getDescription() != null && !workspace.getDescription().isBlank()) {
            sb.append("Description: ").append(workspace.getDescription()).append("\n");
        }
        if (workspace.getOwner() != null) {
            sb.append("Owner: ").append(nullSafe(workspace.getOwner().getFullName())).append("\n");
        }
        sb.append("Created: ").append(
                workspace.getCreatedAt() != null ? workspace.getCreatedAt().format(DATE_FORMAT) : "N/A"
        ).append("\n");
        return sb.toString();
    }

    private String buildProjectsSection(List<Project> projects, List<Task> tasks) {
        StringBuilder sb = new StringBuilder();
        sb.append("PROJECTS OVERVIEW\n");
        sb.append("───────────────────────────────────────────────────────────────────\n");

        if (projects == null || projects.isEmpty()) {
            sb.append("No projects found.\n");
            return sb.toString();
        }

        sb.append("Total Projects: ").append(projects.size()).append("\n\n");

        for (Project project : projects) {
            sb.append("• ").append(nullSafe(project.getName()));
            if (project.getKey() != null) {
                sb.append(" [").append(project.getKey()).append("]");
            }
            sb.append("\n");

            if (project.getDescription() != null && !project.getDescription().isBlank()) {
                sb.append("  Description: ").append(truncate(project.getDescription(), 100)).append("\n");
            }

            if (tasks != null) {
                long totalTasks = tasks.stream()
                        .filter(t -> t.getProject() != null && t.getProject().getId().equals(project.getId()))
                        .count();
                long doneTasks = tasks.stream()
                        .filter(t -> t.getProject() != null && t.getProject().getId().equals(project.getId()))
                        .filter(t -> t.getStatus() == TaskStatus.DONE)
                        .count();
                long overdueTasks = tasks.stream()
                        .filter(t -> t.getProject() != null && t.getProject().getId().equals(project.getId()))
                        .filter(Task::isOverdue)
                        .count();

                sb.append("  Tasks: ").append(doneTasks).append("/").append(totalTasks).append(" completed");
                if (overdueTasks > 0) {
                    sb.append(" (").append(overdueTasks).append(" overdue)");
                }
                sb.append("\n");
            }

            if (project.getColor() != null) {
                sb.append("  Color: ").append(project.getColor()).append("\n");
            }
        }

        return sb.toString();
    }

    private String buildTasksSection(List<Task> tasks) {
        StringBuilder sb = new StringBuilder();
        sb.append("TASKS OVERVIEW\n");
        sb.append("───────────────────────────────────────────────────────────────────\n");

        if (tasks == null || tasks.isEmpty()) {
            sb.append("No tasks found.\n");
            return sb.toString();
        }

        int total = tasks.size();
        int done = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        int inProgress = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        int todo = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.TODO).count();
        int review = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.REVIEW).count();
        int overdue = (int) tasks.stream().filter(Task::isOverdue).count();

        sb.append("Statistics:\n");
        sb.append("  Total: ").append(total).append("\n");
        sb.append("  Done: ").append(done).append("\n");
        sb.append("  In Progress: ").append(inProgress).append("\n");
        sb.append("  To Do: ").append(todo).append("\n");
        sb.append("  In Review: ").append(review).append("\n");
        sb.append("  Overdue: ").append(overdue).append("\n");
        sb.append("  Completion Rate: ").append(total > 0 ? String.format("%.1f%%", (done * 100.0 / total)) : "0%").append("\n\n");

        if (overdue > 0) {
            sb.append("OVERDUE TASKS:\n");
            tasks.stream()
                    .filter(Task::isOverdue)
                    .limit(10)
                    .forEach(t -> {
                        String projectKey = t.getProject() != null && t.getProject().getKey() != null
                                ? t.getProject().getKey() + "-" : "";
                        sb.append("  ⚠ ").append(projectKey).append(t.getTaskNumber())
                                .append(": ").append(nullSafe(t.getTitle()));
                        if (t.getDueDate() != null) {
                            sb.append(" (due: ").append(t.getDueDate().format(DATE_FORMAT)).append(")");
                        }
                        sb.append("\n");
                    });
            sb.append("\n");
        }

        sb.append("HIGH PRIORITY TASKS:\n");
        tasks.stream()
                .filter(t -> t.getPriority() == TaskPriority.HIGH || t.getPriority() == TaskPriority.URGENT)
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .limit(10)
                .forEach(t -> {
                    String projectKey = t.getProject() != null && t.getProject().getKey() != null
                            ? t.getProject().getKey() + "-" : "";
                    sb.append("  🔴 ").append(projectKey).append(t.getTaskNumber())
                            .append(": ").append(nullSafe(t.getTitle()))
                            .append(" [").append(t.getPriority()).append("]\n");
                });

        return sb.toString();
    }

    private String buildGoalsSection(List<Goal> goals) {
        StringBuilder sb = new StringBuilder();
        sb.append("GOALS & OKRs\n");
        sb.append("───────────────────────────────────────────────────────────────────\n");

        if (goals == null || goals.isEmpty()) {
            sb.append("No goals found.\n");
            return sb.toString();
        }

        int totalGoals = goals.size();
        int activeGoals = (int) goals.stream().filter(g -> g.getStatus() == Goal.GoalStatus.ACTIVE).count();
        int achievedGoals = (int) goals.stream().filter(g -> g.getStatus() == Goal.GoalStatus.ACHIEVED).count();
        int behindGoals = (int) goals.stream()
                .filter(g -> g.getStatus() == Goal.GoalStatus.ACTIVE)
                .filter(g -> g.getProgressPercentage() != null && g.getProgressPercentage() < 50)
                .count();

        sb.append("Statistics:\n");
        sb.append("  Total Goals: ").append(totalGoals).append("\n");
        sb.append("  Active: ").append(activeGoals).append("\n");
        sb.append("  Achieved: ").append(achievedGoals).append("\n");
        sb.append("  Behind Schedule: ").append(behindGoals).append("\n\n");

        sb.append("GOALS DETAILS:\n");
        goals.stream()
                .limit(10)
                .forEach(g -> {
                    sb.append("  • ").append(nullSafe(g.getTitle()))
                            .append(" [").append(g.getStatus()).append("]")
                            .append(": ").append(g.getProgressPercentage() != null ? g.getProgressPercentage() : 0).append("%");
                    if (g.getDueDate() != null) {
                        sb.append(" (due: ").append(g.getDueDate().format(DATE_FORMAT)).append(")");
                    }
                    sb.append("\n");
                });

        return sb.toString();
    }

    private String buildMembersSection(List<WorkspaceMember> members, List<MemberWorkload> workloads) {
        StringBuilder sb = new StringBuilder();
        sb.append("TEAM MEMBERS\n");
        sb.append("───────────────────────────────────────────────────────────────────\n");

        if (members == null || members.isEmpty()) {
            sb.append("No members found.\n");
            return sb.toString();
        }

        sb.append("Total Members: ").append(members.size()).append("\n\n");

        for (WorkspaceMember member : members) {
            User user = member.getUser();
            if (user == null) continue;

            sb.append("• ").append(nullSafe(user.getFullName()))
                    .append(" (").append(nullSafe(user.getEmail())).append(")");

            if (member.getRole() != null) {
                sb.append(" - ").append(member.getRole().getName());
            }
            sb.append("\n");

            if (member.getJoinedAt() != null) {
                sb.append("  Joined: ").append(member.getJoinedAt().format(DATE_FORMAT)).append("\n");
            }
        }

        if (workloads != null && !workloads.isEmpty()) {
            sb.append("\nWORKLOAD ANALYSIS (Last 7 Days):\n");

            for (MemberWorkload workload : workloads) {
                if (workload.getMember() == null || workload.getMember().getUser() == null) continue;

                String memberName = workload.getMember().getUser().getFullName();
                double percentage = workload.getWorkloadPercentage() != null ? workload.getWorkloadPercentage() : 0;
                int openTasks = workload.getOpenTasks() != null ? workload.getOpenTasks() : 0;
                int inProgress = workload.getInProgressTasks() != null ? workload.getInProgressTasks() : 0;

                String status = percentage > 100 ? "🔴 OVERLOADED" :
                        percentage > 80 ? "🟡 HIGH" :
                                percentage > 50 ? "🟢 NORMAL" : "⚪ LOW";

                sb.append("  ").append(nullSafe(memberName))
                        .append(": ").append(String.format("%.0f%%", percentage))
                        .append(" (").append(openTasks).append(" open, ").append(inProgress).append(" in progress)")
                        .append(" ").append(status)
                        .append("\n");
            }
        }

        return sb.toString();
    }

    private String buildPagesSection(List<Page> pages) {
        StringBuilder sb = new StringBuilder();
        sb.append("DOCUMENTS (PAGES)\n");
        sb.append("───────────────────────────────────────────────────────────────────\n");

        if (pages == null || pages.isEmpty()) {
            sb.append("No pages found.\n");
            return sb.toString();
        }

        sb.append("Total Pages: ").append(pages.size()).append("\n\n");

        pages.stream()
                .limit(15)
                .forEach(p -> {
                    sb.append("• ").append(nullSafe(p.getTitle()));
                    if (p.getIcon() != null) {
                        sb.append(" ").append(p.getIcon());
                    }
                    if (p.getCreatedBy() != null) {
                        sb.append(" (by ").append(nullSafe(p.getCreatedBy().getFullName())).append(")");
                    }
                    sb.append("\n");
                });

        return sb.toString();
    }

    private String buildRecentActivitySection(List<ActivityLog> activities) {
        StringBuilder sb = new StringBuilder();
        sb.append("RECENT ACTIVITY\n");
        sb.append("───────────────────────────────────────────────────────────────────\n");

        if (activities == null || activities.isEmpty()) {
            sb.append("No recent activity.\n");
            return sb.toString();
        }

        activities.stream()
                .limit(15)
                .forEach(a -> {
                    String user = a.getUser() != null ? a.getUser().getFullName() : "System";
                    sb.append("• ").append(nullSafe(user))
                            .append(" ").append(nullSafe(a.getAction()))
                            .append(" ").append(nullSafe(a.getEntityType()));
                    if (a.getCreatedAt() != null) {
                        sb.append(" at ").append(a.getCreatedAt().format(DATE_FORMAT));
                    }
                    sb.append("\n");
                });

        return sb.toString();
    }

    private String buildQuestionSection(String question) {
        StringBuilder sb = new StringBuilder();
        sb.append("USER QUESTION\n");
        sb.append("───────────────────────────────────────────────────────────────────\n");
        sb.append(question).append("\n");
        return sb.toString();
    }

    private String buildOutputInstructions() {
        return """
                ════════════════════════════════════════════════════════════════════
                OUTPUT REQUIREMENTS
                ════════════════════════════════════════════════════════════════════

                You are a Senior Agile Coach, Technical Lead, and Project Manager.
                Answer the user's question using ONLY the provided workspace data.

                If the information is unavailable or insufficient, respond with:
                "I don't have enough workspace information to answer this question."

                NEVER hallucinate or make up information not present in the context.

                Return ONLY valid JSON with this structure:
                {
                  "answer": "Your comprehensive answer here...",
                  "confidence": 0.0-1.0,
                  "sources": ["source1", "source2"],
                  "relatedProjects": [
                    {"id": "uuid", "name": "Project Name", "type": "project", "status": "status"}
                  ],
                  "relatedTasks": [
                    {"id": "uuid", "name": "Task Title", "type": "task", "status": "DONE/TODO/IN_PROGRESS"}
                  ],
                  "suggestions": ["suggestion 1", "suggestion 2"]
                }

                Rules:
                - confidence: 0.0-1.0 (higher if you have strong data evidence)
                - sources: List the data sources used (projects, tasks, goals, etc.)
                - relatedProjects: Max 5 relevant projects
                - relatedTasks: Max 10 relevant tasks
                - suggestions: 2-5 actionable suggestions
                - Return ONLY JSON, no markdown, no explanation
                """;
    }

    private String nullSafe(String value) {
        return value != null ? value : "N/A";
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
