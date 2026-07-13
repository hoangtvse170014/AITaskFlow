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
public class TaskAssignmentPromptBuilder {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public String build(
            TaskAssignmentData taskData,
            List<MemberData> members,
            Project project) {

        StringBuilder prompt = new StringBuilder();

        prompt.append("You are a Senior Engineering Manager with 15+ years of experience in software development teams.\n\n");
        prompt.append(buildTaskSection(taskData)).append("\n\n");
        prompt.append(buildMembersSection(members)).append("\n\n");
        prompt.append(buildOutputSchema());

        return prompt.toString();
    }

    private String buildTaskSection(TaskAssignmentData task) {
        StringBuilder sb = new StringBuilder();
        sb.append("TASK TO ASSIGN\n");
        sb.append("═══════════════════════════════════════\n");
        sb.append("Title: ").append(nullSafe(task.getTitle())).append("\n");
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            sb.append("Description: ").append(task.getDescription()).append("\n");
        }
        sb.append("Priority: ").append(nullSafe(task.getPriority())).append("\n");
        if (task.getDueDate() != null) {
            sb.append("Due Date: ").append(task.getDueDate().format(DATE_FORMAT)).append("\n");
        }
        if (task.getProjectName() != null) {
            sb.append("Project: ").append(task.getProjectName()).append("\n");
        }
        return sb.toString();
    }

    private String buildMembersSection(List<MemberData> members) {
        StringBuilder sb = new StringBuilder();
        sb.append("TEAM MEMBERS\n");
        sb.append("═══════════════════════════════════════\n");

        if (members == null || members.isEmpty()) {
            sb.append("No team members available.\n");
            return sb.toString();
        }

        for (MemberData member : members) {
            sb.append("\n");
            sb.append("───────────────────────────────────────\n");
            sb.append("Name: ").append(nullSafe(member.getName())).append("\n");
            sb.append("Email: ").append(nullSafe(member.getEmail())).append("\n");
            sb.append("Role: ").append(nullSafe(member.getRole())).append("\n");
            sb.append("Workload: ").append(String.format("%.0f%%", member.getWorkloadPercentage())).append(" (");
            sb.append(member.getOpenTasks()).append(" open, ");
            sb.append(member.getInProgressTasks()).append(" in progress)\n");
            sb.append("Tasks Completed (7d): ").append(member.getCompletedTasksLastWeek()).append("\n");
            sb.append("Total Assigned Tasks: ").append(member.getTotalAssignedTasks()).append("\n");

            if (member.getStatus() != null) {
                String statusIcon = switch (member.getStatus()) {
                    case "OVERLOADED" -> "🔴 ";
                    case "BLOCKED" -> "🟠 ";
                    case "UNDERUTILIZED" -> "🟢 ";
                    default -> "🟡 ";
                };
                sb.append("Status: ").append(statusIcon).append(member.getStatus()).append("\n");
            }
        }

        return sb.toString();
    }

    private String buildOutputSchema() {
        return """
                ASSIGNMENT REQUEST
                ════════════════════════════════════════
                Based on the task requirements and team member data, recommend the best assignee.

                Return ONLY valid JSON with this exact structure:
                {
                  "recommendedAssignee": "member name or null if no suitable member",
                  "recommendedAssigneeId": "member UUID from the ranking or null",
                  "confidence": 0.0-1.0,
                  "ranking": [
                    {
                      "memberId": "member UUID",
                      "memberName": "member name",
                      "email": "member email",
                      "score": 0-100,
                      "currentWorkload": "low|medium|high",
                      "openTasks": 0,
                      "inProgressTasks": 0,
                      "role": "role name",
                      "reason": "brief explanation of why this member"
                    }
                  ],
                  "warnings": ["warning 1", "warning 2"],
                  "reason": "overall reasoning for the recommendation"
                }

                Rules:
                - Score should consider: workload, skills match, task count, recent performance
                - If no member is suitable, recommendAssignee should be null
                - Provide at least top 3 recommendations in ranking
                - Warnings should highlight team issues (overload, skill gaps, etc.)
                - Return ONLY JSON, no markdown or explanation
                """;
    }

    private String nullSafe(String value) {
        return value != null ? value : "N/A";
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class TaskAssignmentData {
        private String title;
        private String description;
        private String priority;
        private LocalDate dueDate;
        private String projectName;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class MemberData {
        private String id;
        private String name;
        private String email;
        private String role;
        private Double workloadPercentage;
        private Integer openTasks;
        private Integer inProgressTasks;
        private Integer completedTasksLastWeek;
        private Integer totalAssignedTasks;
        private String status;
    }
}
