package com.taskflow.ai.service;

import com.taskflow.ai.dto.WorkspaceSnapshot;
import com.taskflow.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds the textual context string that is sent to the LLM together with the
 * system prompt. The output is grouped into named sections so the model can
 * pull only the slices it needs for each question type.
 *
 * <p>Behaviour:
 * <ul>
 *   <li>Always emits the full workspace snapshot, capped to keep token usage
 *       predictable even when the workspace has thousands of tasks.</li>
 *   <li>Every claim in the context is derived directly from the JPA entities
 *       loaded from the database - the model is explicitly told never to
 *       invent people, projects, dates or numbers that are not in this
 *       context.</li>
 * </ul>
 */
@Component
@Slf4j
public class WorkspaceContextBuilder {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final int MAX_TASKS_FULL_DETAIL = 120;
    private static final int MAX_SUBTASKS = 60;
    private static final int MAX_BLOCKS = 30;
    private static final int MAX_ACTIVITIES = 25;
    private static final int MAX_COMMENTS = 15;
    private static final int MAX_PAGES = 25;

    // =====================================================================
    // Public entry point
    // =====================================================================

    public String buildContext(WorkspaceSnapshot snapshot, String question) {
        StringBuilder context = new StringBuilder(8192);

        appendHeader(context);
        appendWorkspaceSection(context, snapshot);
        appendTeamSection(context, snapshot);
        appendProjectsSection(context, snapshot);
        appendSubTasksSection(context, snapshot);
        appendDeadlinesSection(context, snapshot);
        appendBlockersSection(context, snapshot);
        appendGoalsSection(context, snapshot);
        appendPagesSection(context, snapshot);
        appendActivitySection(context, snapshot);
        appendCommentsSection(context, snapshot);
        appendQuestionSection(context, question);
        appendOutputRules(context);

        log.debug("Built workspace context: {} chars (snapshot tasks={}, subtasks={}, members={})",
                context.length(),
                snapshot.getTasks() != null ? snapshot.getTasks().size() : 0,
                snapshot.getSubtasks() != null ? snapshot.getSubtasks().size() : 0,
                snapshot.getMembers() != null ? snapshot.getMembers().size() : 0);
        return context.toString();
    }

    // =====================================================================
    // Section builders
    // =====================================================================

    private void appendHeader(StringBuilder sb) {
        sb.append("=== WORKSPACE AI CONTEXT ===\n");
        sb.append("All data below is the authoritative workspace state pulled from the database.\n");
        sb.append("Answer ONLY using this data. Never invent names, projects, tasks, dates or numbers.\n\n");
    }

    private void appendWorkspaceSection(StringBuilder sb, WorkspaceSnapshot snapshot) {
        Workspace workspace = snapshot.getWorkspace();
        if (workspace == null) return;

        sb.append("## WORKSPACE\n");
        sb.append("- Name: ").append(safe(workspace.getName())).append('\n');
        sb.append("- Slug: /").append(safe(workspace.getSlug())).append('\n');
        if (workspace.getDescription() != null && !workspace.getDescription().isBlank()) {
            sb.append("- Description: ").append(workspace.getDescription().trim()).append('\n');
        }
        if (workspace.getOwner() != null) {
            sb.append("- Owner: ").append(safe(workspace.getOwner().getFullName()))
                    .append(" (").append(safe(workspace.getOwner().getEmail())).append(")\n");
        }
        sb.append("- Created: ").append(formatDate(workspace.getCreatedAt())).append("\n\n");
    }

    private void appendTeamSection(StringBuilder sb, WorkspaceSnapshot snapshot) {
        List<WorkspaceMember> members = snapshot.getMembers();
        List<MemberWorkload> workloads = snapshot.getWorkloads();
        sb.append("## TEAM & WORKLOAD\n");

        if (members == null || members.isEmpty()) {
            sb.append("No active members found.\n\n");
            return;
        }

        Map<UUID, MemberWorkload> workloadByMember = workloads != null
                ? workloads.stream().collect(Collectors.toMap(
                        w -> w.getMember().getId(), w -> w, (a, b) -> a))
                : Collections.emptyMap();

        sb.append("Total Active Members: ").append(members.size()).append('\n');

        Map<String, Long> roleCount = members.stream()
                .filter(m -> m.getRole() != null)
                .collect(Collectors.groupingBy(r -> r.getRole().getName(), Collectors.counting()));
        if (!roleCount.isEmpty()) {
            sb.append("Roles: ").append(roleCount.entrySet().stream()
                    .map(e -> e.getValue() + " " + e.getKey())
                    .sorted()
                    .collect(Collectors.joining(", "))).append('\n');
        }

        for (WorkspaceMember member : members) {
            User user = member.getUser();
            if (user == null) continue;

            MemberWorkload latest = workloadByMember.get(member.getId());
            sb.append("- ").append(safe(user.getFullName()));
            if (member.getRole() != null) {
                sb.append(" [").append(member.getRole().getName()).append(']');
            }
            if (latest != null) {
                int pct = latest.getWorkloadPercentage() != null ? latest.getWorkloadPercentage() : 0;
                String status = latest.getStatus() != null ? latest.getStatus() : "BALANCED";
                sb.append(" - workload ").append(pct).append("% (").append(status).append(')');
                sb.append(", open=").append(nz(latest.getOpenTasks()));
                sb.append(", in_progress=").append(nz(latest.getInProgressTasks()));
                sb.append(", blocked=").append(nz(latest.getBlockedTasks()));
                sb.append(", completed_7d=").append(nz(latest.getCompletedTasks()));
            }
            sb.append('\n');
        }
        sb.append('\n');
    }

    private void appendProjectsSection(StringBuilder sb, WorkspaceSnapshot snapshot) {
        List<Project> projects = snapshot.getProjects();
        List<Task> tasks = snapshot.getTasks();
        sb.append("## PROJECTS & TASKS\n");

        if (projects == null || projects.isEmpty()) {
            sb.append("No projects found.\n\n");
            return;
        }

        Map<UUID, List<Task>> tasksByProject = tasks != null
                ? tasks.stream()
                    .filter(t -> t.getProject() != null)
                    .collect(Collectors.groupingBy(t -> t.getProject().getId()))
                : Collections.emptyMap();

        for (Project project : projects) {
            List<Task> projectTasks = tasksByProject.getOrDefault(project.getId(), List.of());

            int total = projectTasks.size();
            int done = (int) projectTasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
            int inProgress = (int) projectTasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
            int review = (int) projectTasks.stream().filter(t -> t.getStatus() == TaskStatus.REVIEW).count();
            int overdue = (int) projectTasks.stream().filter(Task::isOverdue).count();
            int unassigned = (int) projectTasks.stream()
                    .filter(t -> t.getAssignee() == null && t.getStatus() != TaskStatus.DONE)
                    .count();

            sb.append("### ").append(safe(project.getName()))
                    .append(" [").append(safe(project.getKey())).append("]\n");
            if (project.getDescription() != null && !project.getDescription().isBlank()) {
                sb.append("  Description: ").append(truncate(project.getDescription(), 120)).append('\n');
            }
            sb.append("  Progress: ").append(done).append('/').append(total)
                    .append(" done | in_progress=").append(inProgress)
                    .append(" | review=").append(review)
                    .append(" | overdue=").append(overdue)
                    .append(" | unassigned_open=").append(unassigned).append('\n');

            // Show tasks when not too many
            if (total == 0) {
                sb.append("  Tasks: (none)\n");
            } else if (total <= MAX_TASKS_FULL_DETAIL) {
                for (Task task : projectTasks) {
                    sb.append("  ").append(formatTaskLine(task)).append('\n');
                }
            } else {
                // Aggregate view for huge projects
                Map<TaskStatus, Long> byStatus = projectTasks.stream()
                        .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));
                sb.append("  By status: ");
                sb.append(byStatus.entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .sorted()
                        .collect(Collectors.joining(", "))).append('\n');
                projectTasks.stream()
                        .filter(t -> t.getPriority() == TaskPriority.URGENT || t.getPriority() == TaskPriority.HIGH)
                        .limit(10)
                        .forEach(t -> sb.append("  ! ").append(formatTaskLine(t)).append('\n'));
            }
            sb.append('\n');
        }
    }

    private void appendSubTasksSection(StringBuilder sb, WorkspaceSnapshot snapshot) {
        List<SubTask> subtasks = snapshot.getSubtasks();
        sb.append("## SUBTASKS\n");

        if (subtasks == null || subtasks.isEmpty()) {
            sb.append("No subtasks in this workspace.\n\n");
            return;
        }

        long total = subtasks.size();
        long open = subtasks.stream().filter(s -> !Boolean.TRUE.equals(s.getCompleted())).count();
        sb.append("Total: ").append(total).append(" | Open: ").append(open)
                .append(" | Completed: ").append(total - open).append('\n');

        subtasks.stream()
                .filter(s -> !Boolean.TRUE.equals(s.getCompleted()))
                .limit(MAX_SUBTASKS)
                .forEach(s -> {
                    Task parent = s.getTask();
                    sb.append("  - ").append(safe(s.getTitle()));
                    if (parent != null && parent.getProject() != null) {
                        sb.append(" (parent: ").append(parent.getTaskKey()).append(' ').append(safe(parent.getTitle())).append(')');
                    }
                    sb.append('\n');
                });
        sb.append('\n');
    }

    private void appendDeadlinesSection(StringBuilder sb, WorkspaceSnapshot snapshot) {
        List<Task> tasks = snapshot.getTasks();
        LocalDate today = snapshot.getToday() != null ? snapshot.getToday() : LocalDate.now();
        sb.append("## DEADLINES\n");

        if (tasks == null || tasks.isEmpty()) {
            sb.append("No tasks.\n\n");
            return;
        }

        List<Task> overdue = tasks.stream()
                .filter(Task::isOverdue)
                .sorted(Comparator.comparing(Task::getDueDate))
                .limit(25)
                .toList();
        List<Task> dueThisWeek = tasks.stream()
                .filter(t -> t.getDueDate() != null && !t.isOverdue()
                        && !t.getDueDate().isAfter(today.plusDays(7)))
                .sorted(Comparator.comparing(Task::getDueDate))
                .limit(20)
                .toList();
        List<Task> upcoming = tasks.stream()
                .filter(t -> t.getDueDate() != null && !t.isOverdue()
                        && t.getDueDate().isAfter(today.plusDays(7))
                        && !t.getDueDate().isAfter(today.plusDays(30)))
                .sorted(Comparator.comparing(Task::getDueDate))
                .limit(15)
                .toList();

        sb.append("Today: ").append(today.format(DATE_FORMAT)).append('\n');

        sb.append("OVERDUE (").append(overdue.size()).append("):\n");
        overdue.forEach(t -> sb.append("  ").append(formatTaskLine(t)).append('\n'));

        sb.append("THIS WEEK (").append(dueThisWeek.size()).append("):\n");
        dueThisWeek.forEach(t -> sb.append("  ").append(formatTaskLine(t)).append('\n'));

        sb.append("UPCOMING 30 DAYS (").append(upcoming.size()).append("):\n");
        upcoming.forEach(t -> sb.append("  ").append(formatTaskLine(t)).append('\n'));

        sb.append('\n');
    }

    private void appendBlockersSection(StringBuilder sb, WorkspaceSnapshot snapshot) {
        sb.append("## BLOCKERS\n");

        // Tasks that have been stuck in IN_PROGRESS or REVIEW for a long time
        LocalDateTime now = LocalDateTime.now();
        List<Task> stuck = snapshot.getTasks() != null
                ? snapshot.getTasks().stream()
                    .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS || t.getStatus() == TaskStatus.REVIEW)
                    .filter(t -> t.getUpdatedAt() != null
                            && ChronoUnit.DAYS.between(t.getUpdatedAt(), now) >= 7)
                    .sorted(Comparator.comparing(Task::getUpdatedAt))
                    .limit(15)
                    .toList()
                : List.of();

        // Members flagged BLOCKED in workload records
        List<MemberWorkload> blockedMembers = snapshot.getWorkloads() != null
                ? snapshot.getWorkloads().stream()
                    .filter(w -> "BLOCKED".equalsIgnoreCase(w.getStatus()))
                    .limit(10)
                    .toList()
                : List.of();

        // Open subtasks attached to overdue parents are also treated as implicit blockers
        long blockedSubtasks = snapshot.getSubtasks() != null
                ? snapshot.getSubtasks().stream()
                    .filter(s -> !Boolean.TRUE.equals(s.getCompleted()))
                    .filter(s -> s.getTask() != null && s.getTask().isOverdue())
                    .count()
                : 0L;

        if (stuck.isEmpty() && blockedMembers.isEmpty() && blockedSubtasks == 0) {
            sb.append("No notable blockers detected.\n\n");
            return;
        }

        if (!stuck.isEmpty()) {
            sb.append("Stuck tasks (no progress for 7+ days):\n");
            stuck.forEach(t -> sb.append("  ").append(formatTaskLine(t))
                    .append(" — last updated ").append(formatRelative(t.getUpdatedAt())).append('\n'));
        }
        if (!blockedMembers.isEmpty()) {
            sb.append("Members flagged BLOCKED:\n");
            blockedMembers.forEach(w -> {
                if (w.getMember() != null && w.getMember().getUser() != null) {
                    sb.append("  - ").append(safe(w.getMember().getUser().getFullName()))
                            .append(" blocked=").append(nz(w.getBlockedTasks()))
                            .append(", workload=").append(nz(w.getWorkloadPercentage())).append("%\n");
                }
            });
        }
        if (blockedSubtasks > 0) {
            sb.append("Open subtasks of overdue parent tasks: ").append(blockedSubtasks).append('\n');
        }
        sb.append('\n');
    }

    private void appendGoalsSection(StringBuilder sb, WorkspaceSnapshot snapshot) {
        List<Goal> goals = snapshot.getGoals();
        sb.append("## GOALS & OKRs\n");
        if (goals == null || goals.isEmpty()) {
            sb.append("No goals defined.\n\n");
            return;
        }

        long active = goals.stream().filter(g -> g.getStatus() == Goal.GoalStatus.ACTIVE).count();
        long achieved = goals.stream().filter(g -> g.getStatus() == Goal.GoalStatus.ACHIEVED).count();
        sb.append("Total: ").append(goals.size()).append(" | Active: ").append(active)
                .append(" | Achieved: ").append(achieved).append('\n');

        for (Goal goal : goals) {
            sb.append("- ").append(safe(goal.getTitle()));
            sb.append(" [").append(goal.getStatus()).append("] ");
            sb.append(goal.getProgressPercentage() != null ? goal.getProgressPercentage() : 0).append('%');
            if (goal.getDueDate() != null) {
                sb.append(" (due ").append(goal.getDueDate().format(DATE_FORMAT)).append(')');
            }
            if (goal.getOwner() != null) {
                sb.append(" owner=").append(safe(goal.getOwner().getFullName()));
            }
            sb.append('\n');
            if (goal.getKeyResults() != null && !goal.getKeyResults().isEmpty()) {
                goal.getKeyResults().forEach(kr -> sb.append("    KR: ").append(safe(kr.getTitle()))
                        .append(" (").append(kr.getProgressPercentage() != null ? kr.getProgressPercentage() : 0)
                        .append("%)\n"));
            }
        }
        sb.append('\n');
    }

    private void appendPagesSection(StringBuilder sb, WorkspaceSnapshot snapshot) {
        List<Page> pages = snapshot.getPages();
        List<Block> blocks = snapshot.getRecentBlocks();
        sb.append("## DOCUMENTS / PAGES\n");

        if ((pages == null || pages.isEmpty()) && (blocks == null || blocks.isEmpty())) {
            sb.append("No documents found.\n\n");
            return;
        }

        if (pages != null) {
            sb.append("Pages (").append(pages.size()).append("):\n");
            pages.stream().limit(MAX_PAGES).forEach(p ->
                    sb.append("  - ").append(safe(p.getTitle()))
                            .append(p.getCreatedBy() != null ? " (by " + safe(p.getCreatedBy().getFullName()) + ")" : "")
                            .append('\n'));
        }

        if (blocks != null && !blocks.isEmpty()) {
            sb.append("Recent document content snippets:\n");
            blocks.stream()
                    .filter(b -> b.getContent() != null && !b.getContent().isBlank())
                    .limit(MAX_BLOCKS)
                    .forEach(b -> {
                        String title = b.getPage() != null ? b.getPage().getTitle() : "?";
                        sb.append("  [").append(safe(title)).append("] ")
                                .append(truncate(b.getContent(), 160).replace('\n', ' '))
                                .append('\n');
                    });
        }
        sb.append('\n');
    }

    private void appendActivitySection(StringBuilder sb, WorkspaceSnapshot snapshot) {
        List<ActivityLog> activities = snapshot.getRecentActivities();
        sb.append("## RECENT ACTIVITY\n");

        if (activities == null || activities.isEmpty()) {
            sb.append("No recent activity.\n\n");
            return;
        }

        activities.stream().limit(MAX_ACTIVITIES).forEach(a -> {
            String actor = a.getUser() != null ? a.getUser().getFullName() : "System";
            sb.append("- ").append(safe(actor))
                    .append(' ').append(safe(a.getAction()))
                    .append(' ').append(safe(a.getEntityType()))
                    .append(" (").append(formatRelative(a.getCreatedAt())).append(")\n");
        });
        sb.append('\n');
    }

    private void appendCommentsSection(StringBuilder sb, WorkspaceSnapshot snapshot) {
        List<TaskComment> comments = snapshot.getRecentComments();
        sb.append("## RECENT DISCUSSIONS\n");

        if (comments == null || comments.isEmpty()) {
            sb.append("No recent task discussions.\n\n");
            return;
        }

        comments.stream().limit(MAX_COMMENTS).forEach(c -> {
            Task task = c.getTask();
            sb.append("- ");
            if (c.getUser() != null) sb.append(safe(c.getUser().getFullName())).append(" on ");
            if (task != null) sb.append(task.getTaskKey()).append(' ').append(safe(task.getTitle()));
            sb.append(": ").append(truncate(c.getContent(), 140).replace('\n', ' ')).append('\n');
        });
        sb.append('\n');
    }

    private void appendQuestionSection(StringBuilder sb, String question) {
        sb.append("## USER QUESTION\n");
        sb.append(question == null ? "(empty)" : question.trim()).append("\n\n");
    }

    private void appendOutputRules(StringBuilder sb) {
        sb.append("## OUTPUT RULES\n");
        sb.append("- Use ONLY information from the sections above. Do not invent people, dates, percentages, or items.\n");
        sb.append("- If the data does not contain the answer, say exactly:\n");
        sb.append("  \"I don't have enough workspace information to answer this question.\"\n");
        sb.append("- Cite the section you used (e.g. \"Source: TEAM & WORKLOAD\").\n");
        sb.append("- Prefer markdown tables and bullet lists for ranked or tabular answers.\n");
        sb.append("- Reference real entity ids / keys from the context in the relatedProjects / relatedTasks arrays.\n");
        sb.append("- Return ONLY a single JSON object with this exact shape:\n");
        sb.append("{\n");
        sb.append("  \"answer\": \"<markdown answer>\",\n");
        sb.append("  \"confidence\": 0.0-1.0,\n");
        sb.append("  \"sources\": [\"WORKSPACE\",\"TEAM & WORKLOAD\",\"PROJECTS & TASKS\",\"SUBTASKS\",\"DEADLINES\",\"BLOCKERS\",\"GOALS & OKRs\",\"DOCUMENTS / PAGES\",\"RECENT ACTIVITY\",\"RECENT DISCUSSIONS\"],\n");
        sb.append("  \"relatedProjects\": [{\"id\":\"...\",\"name\":\"...\",\"type\":\"project\",\"status\":\"...\",\"description\":\"...\"}],\n");
        sb.append("  \"relatedTasks\": [{\"id\":\"...\",\"name\":\"...\",\"type\":\"task\",\"status\":\"...\",\"description\":\"...\"}],\n");
        sb.append("  \"relatedMembers\": [{\"id\":\"...\",\"name\":\"...\",\"type\":\"member\",\"status\":\"...\",\"description\":\"...\"}],\n");
        sb.append("  \"relatedGoals\": [{\"id\":\"...\",\"name\":\"...\",\"type\":\"goal\",\"status\":\"...\",\"description\":\"...\"}],\n");
        sb.append("  \"relatedPages\": [{\"id\":\"...\",\"name\":\"...\",\"type\":\"page\",\"status\":\"...\",\"description\":\"...\"}],\n");
        sb.append("  \"suggestions\": [\"...\",\"...\"]\n");
        sb.append("}\n");
        sb.append("- Do NOT wrap the JSON in ``` fences. Do NOT add any explanation outside the JSON.\n");
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private String formatTaskLine(Task task) {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(task.getTaskKey()).append("] ");
        sb.append(safe(task.getTitle()));
        sb.append(" | status=").append(task.getStatus());
        sb.append(" | priority=").append(task.getPriority());
        if (task.getAssignee() != null) {
            sb.append(" | assignee=").append(safe(task.getAssignee().getFullName()));
        } else if (task.getStatus() != TaskStatus.DONE) {
            sb.append(" | assignee=UNASSIGNED");
        }
        if (task.getDueDate() != null) {
            sb.append(" | due=").append(task.getDueDate().format(DATE_FORMAT));
            if (task.isOverdue()) sb.append(" (OVERDUE)");
        }
        if (task.getLabels() != null && !task.getLabels().isEmpty()) {
            sb.append(" | labels=").append(task.getLabels().stream()
                    .map(l -> l.getName() != null ? l.getName() : "")
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(",")));
        }
        if (task.getChecklist() != null && !task.getChecklist().isEmpty()) {
            long total = task.getChecklist().size();
            long done = task.getChecklist().stream().filter(c -> c.isCompleted()).count();
            sb.append(" | checklist=").append(done).append('/').append(total);
        }
        return sb.toString();
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMAT) : "N/A";
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMAT) : "N/A";
    }

    private String formatRelative(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        long minutes = ChronoUnit.MINUTES.between(dateTime, LocalDateTime.now());
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        if (days < 30) return days + "d ago";
        long months = days / 30;
        return months + "mo ago";
    }

    private String safe(String value) {
        return value != null ? value : "N/A";
    }

    private int nz(Integer value) {
        return value != null ? value : 0;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        String collapsed = text.replaceAll("\\s+", " ").trim();
        if (collapsed.length() <= maxLength) return collapsed;
        return collapsed.substring(0, maxLength - 3) + "...";
    }
}
