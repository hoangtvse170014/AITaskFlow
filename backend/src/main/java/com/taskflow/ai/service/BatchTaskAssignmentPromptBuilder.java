package com.taskflow.ai.service;

import com.taskflow.ai.dto.BatchTaskAssignmentRequest;
import com.taskflow.entity.Project;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class BatchTaskAssignmentPromptBuilder {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public String build(
            Project project,
            List<BatchTaskAssignmentRequest.TaskInput> tasks,
            List<TaskAssignmentPromptBuilder.MemberData> members) {

        StringBuilder sb = new StringBuilder();
        sb.append("ASSIGN ALL TASKS to the best-fit team members below.\n\n");
        sb.append(buildProjectSection(project));
        sb.append("\n");
        sb.append(buildTasksSection(tasks));
        sb.append("\n");
        sb.append(buildMembersSection(members));
        sb.append("\n");
        sb.append(buildRulesAndSchema());
        return sb.toString();
    }

    private String buildProjectSection(Project project) {
        StringBuilder sb = new StringBuilder();
        sb.append("PROJECT\n");
        sb.append("═══════════════════════════════════════\n");
        sb.append("Name: ").append(project.getName()).append("\n");
        if (project.getDescription() != null) {
            sb.append("Description: ").append(project.getDescription()).append("\n");
        }
        if (project.getCreatedAt() != null) {
            sb.append("Created: ").append(project.getCreatedAt().toLocalDate().format(DATE_FORMAT)).append("\n");
        }
        return sb.toString();
    }

    private String buildTasksSection(List<BatchTaskAssignmentRequest.TaskInput> tasks) {
        StringBuilder sb = new StringBuilder();
        sb.append("TASKS TO ASSIGN (you MUST assign every task)\n");
        sb.append("═══════════════════════════════════════\n");
        for (int i = 0; i < tasks.size(); i++) {
            BatchTaskAssignmentRequest.TaskInput t = tasks.get(i);
            sb.append("\nTask #").append(i + 1).append("\n");
            sb.append("  Ref: ").append(nullSafe(t.getTaskRef())).append("\n");
            sb.append("  Title: ").append(nullSafe(t.getTitle())).append("\n");
            if (t.getDescription() != null && !t.getDescription().isBlank()) {
                sb.append("  Description: ").append(t.getDescription()).append("\n");
            }
            sb.append("  Priority: ").append(nullSafe(t.getPriority())).append("\n");
            if (t.getDueDate() != null) {
                sb.append("  Due Date: ").append(t.getDueDate().format(DATE_FORMAT));
                long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), t.getDueDate());
                sb.append(" (").append(days).append(" day(s) from now)\n");
            }
            if (t.getCategory() != null && !t.getCategory().isBlank()) {
                sb.append("  Category Hint: ").append(t.getCategory()).append("\n");
            }
        }
        return sb.toString();
    }

    private String buildMembersSection(List<TaskAssignmentPromptBuilder.MemberData> members) {
        StringBuilder sb = new StringBuilder();
        sb.append("TEAM MEMBERS\n");
        sb.append("═══════════════════════════════════════\n");
        if (members == null || members.isEmpty()) {
            sb.append("No team members available.\n");
            return sb.toString();
        }
        for (TaskAssignmentPromptBuilder.MemberData m : members) {
            sb.append("\n");
            sb.append("• ID: ").append(nullSafe(m.getId())).append("\n");
            sb.append("  Name: ").append(nullSafe(m.getName())).append("\n");
            sb.append("  Email: ").append(nullSafe(m.getEmail())).append("\n");
            sb.append("  Role: ").append(nullSafe(m.getRole())).append("\n");
            sb.append("  Workload: ").append(String.format("%.0f%%", m.getWorkloadPercentage()));
            sb.append(" (open=").append(m.getOpenTasks());
            sb.append(", in_progress=").append(m.getInProgressTasks());
            sb.append(", completed_last_week=").append(m.getCompletedTasksLastWeek());
            sb.append(", total_assigned=").append(m.getTotalAssignedTasks()).append(")\n");
            if (m.getStatus() != null) {
                sb.append("  Status: ").append(m.getStatus()).append("\n");
            }
        }
        return sb.toString();
    }

    private String buildRulesAndSchema() {
        return """
                ASSIGNMENT RULES (MUST FOLLOW)
                ═══════════════════════════════════════
                1. Every task listed above MUST be assigned. Set assignedMemberId to null ONLY if truly no member can do it, and explain in reason.
                2. SKILL-BASED ROUTING (strict):
                   - "backend", "api", "server", "database", "Spring", "Java", "Node", "Go", etc. -> assign to member whose Role contains: Backend / Backend Developer / Server.
                   - "frontend", "react", "vue", "angular", "client side", state management -> assign to member whose Role contains: Frontend / Frontend Developer / Web Developer.
                   - "ui", "ux", "design", "figma", "wireframe", styling, css, tailwind -> assign to member whose Role contains: UI / UX / Designer.
                   - "document", "documentation", "readme", "wiki", "spec", "rfc", "user guide" -> assign to member whose Role contains: Technical Writer / Writer / Documentation.
                   - If a member's role matches the required skill, prefer them EVEN if their workload is slightly higher.
                3. WORKLOAD BALANCING:
                   - Sort candidate members by currentWorkload ascending; prefer the least loaded qualified member.
                   - Avoid overload: do not assign a task to a member already OVERLOADED (>90% workload) unless they are the only qualified option.
                   - Spread the tasks across the team; do not pile many tasks onto one qualified member if other qualified members are available.
                4. DEADLINE AWARENESS:
                   - High priority / near-deadline tasks go to the least loaded qualified member.
                   - If a due date is within 3 days and the priority is HIGH or CRITICAL, do not assign to anyone with openTasks >= 4.
                5. CONFIDENCE:
                   - confidence (0.0-1.0) reflects how good a fit the assignment is.
                   - 0.9+ = perfect skill match + low load, 0.7+ = good match, 0.5+ = acceptable, <0.5 = poor fit (still must assign if possible).
                6. NEVER assign to a member whose role is clearly unrelated to the task domain (e.g. UI designer for a backend API task). If no qualified member exists, set assignedMemberId = null.

                OUTPUT FORMAT (return ONLY valid JSON, no markdown):
                {
                  "assignments": [
                    {
                      "taskRef": "<the taskRef from input>",
                      "title": "<task title>",
                      "assignedMemberId": "<member UUID from the list, or null>",
                      "assignedMemberName": "<member name or null>",
                      "roleMatched": "<BACKEND | FRONTEND | UI | DOCUMENTATION | FULLSTACK | DEVOPS | QA | OTHER>",
                      "confidence": 0.0,
                      "reason": "<one short sentence explaining the choice>",
                      "unassigned": false
                    }
                  ],
                  "workloadSummary": [
                    {
                      "memberId": "<uuid>",
                      "memberName": "<name>",
                      "assignedTaskCount": 0,
                      "estimatedNewWorkloadPercent": 0.0
                    }
                  ],
                  "overallConfidence": 0.0,
                  "warnings": ["..."],
                  "reason": "<overall team-level reasoning>"
                }
                """;
    }

    private String nullSafe(String value) {
        return value != null ? value : "N/A";
    }
}