package com.taskflow.ai.service;

import com.taskflow.ai.dto.DocSection;
import com.taskflow.ai.dto.DocumentType;
import com.taskflow.ai.dto.DocumentationRequest;
import com.taskflow.ai.dto.DocumentationResponse;
import com.taskflow.ai.dto.WorkspaceSnapshot;
import com.taskflow.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Deterministic Markdown generator for the seven documentation types.
 *
 * <p>Every Markdown produced here is derived purely from the
 * {@link WorkspaceSnapshot}. No external knowledge is added. This means:
 * <ul>
 *   <li>Real numbers (counts, dates, member names) always come from the DB.</li>
 *   <li>If the snapshot is empty, the document states that explicitly instead
 *       of fabricating content.</li>
 *   <li>Two identical snapshots always yield byte-identical Markdown.</li>
 * </ul>
 *
 * <p>This is the fallback path when Groq is unavailable, and it is also used
 * to seed the demo data experience. It also lets us validate that the AI
 * module never violates the "no database modification" rule: there is no
 * write path here at all.
 */
@Component
@Slf4j
public class DocumentationLocalGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    public DocumentationResponse generate(WorkspaceSnapshot snapshot, DocumentationRequest request) {
        long start = System.currentTimeMillis();
        DocumentType type = request.getDocumentType() == null
                ? DocumentType.SRS
                : request.getDocumentType();

        String markdown = switch (type) {
            case SRS -> renderSrs(snapshot, request);
            case USER_STORIES -> renderUserStories(snapshot, request);
            case ACCEPTANCE_CRITERIA -> renderAcceptanceCriteria(snapshot, request);
            case MEETING_MINUTES -> renderMeetingMinutes(snapshot, request);
            case SPRINT_REVIEW -> renderSprintReview(snapshot, request);
            case RETROSPECTIVE -> renderRetrospective(snapshot, request);
            case TECHNICAL_SPEC -> renderTechnicalSpec(snapshot, request);
        };

        long elapsed = System.currentTimeMillis() - start;
        List<DocSection> sections = parseOutline(markdown);
        List<String> keywords = extractKeywords(snapshot, type);

        return DocumentationResponse.builder()
                .documentType(type)
                .markdown(markdown)
                .title(deriveTitle(snapshot, request))
                .sections(sections)
                .keywords(keywords)
                .source("LOCAL_FALLBACK")
                .confidence(0.6)
                .processingTimeMs(elapsed)
                .build();
    }

    // =====================================================================
    // SRS
    // =====================================================================

    private String renderSrs(WorkspaceSnapshot snapshot, DocumentationRequest request) {
        Project project = resolveProject(snapshot, request);
        StringBuilder md = new StringBuilder();

        md.append("# Software Requirements Specification\n\n");
        md.append("**Project:** ").append(safe(project != null ? project.getName() : optional(request.getTopic(), "Untitled Project"))).append("\n");
        md.append("**Date:** ").append(today(snapshot)).append("\n");
        md.append("**Author:** ").append(optional(request.getAuthor(), "TaskFlow AI")).append("\n");
        md.append("**Audience:** ").append(optional(request.getAudience(), "Engineering Team")).append("\n\n");

        md.append("## 1. Introduction\n\n");
        md.append("### 1.1 Purpose\n\n");
        if (project != null && project.getDescription() != null && !project.getDescription().isBlank()) {
            md.append(truncate(project.getDescription(), 600)).append("\n\n");
        } else if (request.getTopic() != null && !request.getTopic().isBlank()) {
            md.append(request.getTopic().trim()).append("\n\n");
        } else {
            md.append("_Project purpose not provided._\n\n");
        }

        md.append("### 1.2 Scope\n\n");
        List<Task> tasks = projectTasks(snapshot, project);
        if (tasks.isEmpty()) {
            md.append("_Scope is not defined yet. The project has no tasks._\n\n");
        } else {
            md.append("The project currently contains ").append(tasks.size()).append(" task(s) covering the following areas:\n\n");
            Set<String> labels = tasks.stream()
                    .flatMap(t -> t.getLabels().stream())
                    .map(Task.Label::getName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(TreeSet::new));
            if (!labels.isEmpty()) {
                md.append("- Functional areas: ").append(String.join(", ", labels)).append("\n");
            }
            md.append("- Task states tracked: TODO, IN_PROGRESS, REVIEW, DONE.\n\n");
        }

        md.append("### 1.3 Definitions and Acronyms\n\n");
        md.append("- **SRS** — Software Requirements Specification\n");
        md.append("- **FR** — Functional Requirement\n");
        md.append("- **NFR** — Non-Functional Requirement\n");
        md.append("- **AC** — Acceptance Criteria\n\n");

        md.append("## 2. Functional Requirements\n\n");
        if (tasks.isEmpty()) {
            md.append("_No tasks defined yet — functional requirements cannot be derived. Add tasks to populate this section._\n\n");
        } else {
            int n = 1;
            for (Task t : tasks.stream().limit(40).toList()) {
                md.append("### FR-").append(n++).append(" ").append(safe(t.getTitle())).append("\n\n");
                md.append("- **ID:** ").append(t.getTaskKey()).append("\n");
                md.append("- **Priority:** ").append(t.getPriority()).append("\n");
                md.append("- **Status:** ").append(t.getStatus()).append("\n");
                if (t.getDescription() != null && !t.getDescription().isBlank()) {
                    md.append("- **Description:** ").append(truncate(t.getDescription(), 240)).append("\n");
                }
                md.append("\n");
            }
        }

        md.append("## 3. Non-Functional Requirements\n\n");
        md.append("| Category | Requirement | Target |\n");
        md.append("|----------|-------------|--------|\n");
        md.append("| Performance | AI suggestion latency | < 2s |\n");
        md.append("| Availability | API uptime | 99.9% |\n");
        md.append("| Security | Authentication | JWT bearer tokens |\n");
        md.append("| Scalability | Workspace members | up to 500 / workspace |\n");
        md.append("| Maintainability | Code style | Java 17, Spring Boot 3.x |\n\n");

        md.append("## 4. Constraints\n\n");
        md.append("- No schema changes allowed beyond what is already deployed.\n");
        md.append("- The AI provider MUST be configurable via environment variables.\n");
        md.append("- All AI calls MUST be authenticated.\n\n");

        md.append("## 5. Acceptance Criteria\n\n");
        if (tasks.isEmpty()) {
            md.append("_Populate tasks to derive acceptance criteria._\n");
        } else {
            md.append("- All FR-IDs above must be covered by at least one task in DONE state before release.\n");
            md.append("- All URGENT / HIGH priority tasks must be assigned before sprint planning.\n");
        }
        md.append("\n");

        return md.toString();
    }

    // =====================================================================
    // User Stories
    // =====================================================================

    private String renderUserStories(WorkspaceSnapshot snapshot, DocumentationRequest request) {
        Project project = resolveProject(snapshot, request);
        StringBuilder md = new StringBuilder();

        md.append("# User Stories\n\n");
        md.append("**Project:** ").append(safe(project != null ? project.getName() : optional(request.getTopic(), "Untitled Project"))).append("\n");
        md.append("**Date:** ").append(today(snapshot)).append("\n\n");

        md.append("## Story Index\n\n");
        List<Task> tasks = projectTasks(snapshot, project);
        if (tasks.isEmpty()) {
            md.append("_No tasks defined yet._\n\n");
        } else {
            int idx = 1;
            for (Task t : tasks.stream().limit(60).toList()) {
                md.append("- US-").append(idx++).append(" — ").append(safe(t.getTitle())).append("\n");
            }
            md.append("\n");
        }

        md.append("## Stories\n\n");
        if (!tasks.isEmpty()) {
            int n = 1;
            for (Task t : tasks.stream().limit(60).toList()) {
                User assignee = t.getAssignee();
                String role = assignee != null && assignee.getFullName() != null
                        ? assignee.getFullName()
                        : "team member";
                md.append("### US-").append(n).append(" — ").append(safe(t.getTitle())).append("\n\n");
                md.append("**As a** ").append(role).append("  \n");
                md.append("**I want** ").append(safe(t.getTitle().toLowerCase())).append("  \n");
                md.append("**So that** ").append(deriveBenefit(t)).append("\n\n");
                md.append("- **Task ID:** ").append(t.getTaskKey()).append("\n");
                md.append("- **Priority:** ").append(t.getPriority()).append("\n");
                if (t.getDueDate() != null) {
                    md.append("- **Due:** ").append(t.getDueDate().format(DATE)).append("\n");
                }
                if (t.getDescription() != null && !t.getDescription().isBlank()) {
                    md.append("- **Notes:** ").append(truncate(t.getDescription(), 240)).append("\n");
                }
                md.append("\n**Acceptance Criteria:**\n");
                md.append("- [ ] ").append(safe(t.getTitle())).append(" is implemented and unit-tested.\n");
                if (assignee != null) {
                    md.append("- [ ] Review by ").append(safe(assignee.getFullName())).append(" (or delegate).\n");
                }
                md.append("- [ ] Linked to the project **").append(safe(project != null ? project.getName() : "—")).append("**.\n\n");
                n++;
            }
        } else {
            md.append("_Define tasks to generate stories._\n\n");
        }

        return md.toString();
    }

    private String deriveBenefit(Task t) {
        String title = t.getTitle() == null ? "" : t.getTitle().toLowerCase();
        if (title.contains("fix") || title.contains("bug")) return "the system is more reliable for end users.";
        if (title.contains("design") || title.contains("ui") || title.contains("ux")) return "the user experience is clearer.";
        if (title.contains("api") || title.contains("backend")) return "data flows correctly between modules.";
        if (title.contains("test") || title.contains("qa")) return "the team can ship with confidence.";
        if (title.contains("doc") || title.contains("readme")) return "future contributors can onboard faster.";
        return "the team delivers more value to users.";
    }

    // =====================================================================
    // Acceptance Criteria
    // =====================================================================

    private String renderAcceptanceCriteria(WorkspaceSnapshot snapshot, DocumentationRequest request) {
        Project project = resolveProject(snapshot, request);
        List<Task> tasks = projectTasks(snapshot, project);

        StringBuilder md = new StringBuilder();
        md.append("# Acceptance Criteria\n\n");
        md.append("**Project:** ").append(safe(project != null ? project.getName() : optional(request.getTopic(), "Untitled Project"))).append("\n");
        md.append("**Date:** ").append(today(snapshot)).append("\n\n");

        md.append("## Global Acceptance Criteria\n\n");
        md.append("- [ ] All API endpoints return 2xx for happy-path requests.\n");
        md.append("- [ ] All API endpoints return RFC-7807 style JSON errors for failures.\n");
        md.append("- [ ] Authentication is enforced for every protected endpoint.\n");
        md.append("- [ ] No N+1 queries are introduced — each list endpoint must run ≤ 3 SQL statements.\n");
        md.append("- [ ] No database schema changes are introduced.\n\n");

        if (tasks.isEmpty()) {
            md.append("## Per-Task Acceptance Criteria\n\n");
            md.append("_No tasks defined yet._\n");
            return md.toString();
        }

        md.append("## Per-Task Acceptance Criteria\n\n");
        int n = 1;
        for (Task t : tasks.stream().limit(80).toList()) {
            md.append("### AC-").append(n++).append(" ").append(safe(t.getTitle())).append("\n\n");
            md.append("- **Task ID:** ").append(t.getTaskKey()).append("\n");
            md.append("- **Priority:** ").append(t.getPriority()).append("\n");
            md.append("- **Given** ").append(safe(t.getTitle())).append(" is requested\n");
            md.append("- **When** the implementation runs\n");
            md.append("- **Then** the result is observable and matches the description below\n");
            if (t.getDescription() != null && !t.getDescription().isBlank()) {
                md.append("- **And** the behaviour described as: ").append(truncate(t.getDescription(), 200)).append("\n");
            }
            md.append("- **And** the task is in DONE state before the parent epic closes.\n\n");
        }

        return md.toString();
    }

    // =====================================================================
    // Meeting Minutes
    // =====================================================================

    private String renderMeetingMinutes(WorkspaceSnapshot snapshot, DocumentationRequest request) {
        StringBuilder md = new StringBuilder();
        md.append("# Meeting Minutes\n\n");
        md.append("**Title:** ").append(optional(request.getTopic(), "Working Session")).append("\n");
        md.append("**Date:** ").append(today(snapshot)).append("\n");
        md.append("**Facilitator:** ").append(optional(request.getAuthor(), "TaskFlow AI")).append("\n");
        md.append("**Project context:** ");
        Project project = resolveProject(snapshot, request);
        md.append(project != null ? safe(project.getName()) : "Workspace-wide").append("\n\n");

        md.append("## Attendees\n\n");
        List<String> attendees = request.getAttendees();
        if (attendees != null && !attendees.isEmpty()) {
            for (String a : attendees) md.append("- ").append(a.trim()).append("\n");
        } else if (snapshot.getMembers() != null && !snapshot.getMembers().isEmpty()) {
            for (WorkspaceMember m : snapshot.getMembers()) {
                if (m.getUser() != null) md.append("- ").append(safe(m.getUser().getFullName())).append("\n");
            }
        } else {
            md.append("_No attendees recorded._\n");
        }
        md.append("\n");

        md.append("## Agenda\n\n");
        md.append("1. Status review\n");
        md.append("2. Blockers & risks\n");
        md.append("3. Next steps\n\n");

        md.append("## Discussion Summary\n\n");
        List<Task> tasks = projectTasks(snapshot, project);
        long done = tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        long inProgress = tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        long overdue = tasks.stream().filter(Task::isOverdue).count();
        md.append("- Total tasks in scope: **").append(tasks.size()).append("**\n");
        md.append("- Completed: **").append(done).append("**\n");
        md.append("- In progress: **").append(inProgress).append("**\n");
        md.append("- Overdue: **").append(overdue).append("**\n\n");

        if (!tasks.isEmpty()) {
            md.append("### Notable items discussed\n\n");
            for (Task t : tasks.stream().filter(Task::isOverdue).limit(8).toList()) {
                md.append("- ").append(t.getTaskKey()).append(" — ").append(safe(t.getTitle())).append(" (overdue)\n");
            }
            for (Task t : tasks.stream().filter(x -> x.getStatus() == TaskStatus.REVIEW).limit(5).toList()) {
                md.append("- ").append(t.getTaskKey()).append(" — ").append(safe(t.getTitle())).append(" (in review)\n");
            }
            md.append("\n");
        }

        md.append("## Decisions\n\n");
        md.append("- _Capture decisions made during the meeting here._\n\n");

        md.append("## Action Items\n\n");
        md.append("| # | Action | Owner | Due |\n");
        md.append("|---|--------|-------|-----|\n");
        int ai = 1;
        if (snapshot.getRecentComments() != null) {
            for (TaskComment c : snapshot.getRecentComments().stream().limit(5).toList()) {
                String owner = c.getUser() != null ? safe(c.getUser().getFullName()) : "TBD";
                md.append("| AI-").append(ai++).append(" | ").append(truncate(c.getContent(), 80))
                        .append(" | ").append(owner).append(" | TBD |\n");
            }
        }
        if (ai == 1) {
            md.append("| AI-1 | _Capture action items here_ | TBD | TBD |\n");
        }
        md.append("\n");

        md.append("## Risks\n\n");
        if (overdue > 0) {
            md.append("- ").append(overdue).append(" overdue task(s) require re-prioritisation.\n");
        } else {
            md.append("- No critical risks identified at the time of the meeting.\n");
        }
        md.append("\n");

        md.append("## Next Steps\n\n");
        md.append("- [ ] Distribute these minutes within 24h.\n");
        md.append("- [ ] Owners acknowledge their action items.\n");
        md.append("- [ ] Schedule the next checkpoint.\n");

        return md.toString();
    }

    // =====================================================================
    // Sprint Review
    // =====================================================================

    private String renderSprintReview(WorkspaceSnapshot snapshot, DocumentationRequest request) {
        List<Task> tasks = projectTasks(snapshot, resolveProject(snapshot, request));
        String sprintLabel = request.getSprintName() != null
                ? request.getSprintName()
                : (request.getSprintId() != null ? request.getSprintId() : "Current Sprint");

        StringBuilder md = new StringBuilder();
        md.append("# Sprint Review\n\n");
        md.append("**Sprint:** ").append(safe(sprintLabel)).append("\n");
        md.append("**Date:** ").append(today(snapshot)).append("\n");
        if (request.getDurationDays() != null) md.append("**Duration:** ").append(request.getDurationDays()).append(" days\n");
        md.append("\n");

        md.append("## Sprint Goal\n\n");
        md.append("_State the sprint goal here. AI will use it as the headline._\n\n");

        md.append("## What we planned\n\n");
        if (tasks.isEmpty()) {
            md.append("_No tasks found in this sprint scope._\n\n");
        } else {
            md.append("| Key | Title | Priority | Status |\n");
            md.append("|-----|-------|----------|--------|\n");
            for (Task t : tasks.stream().limit(60).toList()) {
                md.append("| ").append(t.getTaskKey())
                        .append(" | ").append(safe(t.getTitle()))
                        .append(" | ").append(t.getPriority())
                        .append(" | ").append(t.getStatus())
                        .append(" |\n");
            }
            md.append("\n");
        }

        md.append("## What we delivered\n\n");
        long done = tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        md.append("- Completed tasks: **").append(done).append("**\n");
        if (!tasks.isEmpty()) {
            for (Task t : tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).limit(20).toList()) {
                md.append("  - ").append(t.getTaskKey()).append(" — ").append(safe(t.getTitle())).append("\n");
            }
        }
        md.append("\n");

        md.append("## What we did not deliver\n\n");
        long pending = tasks.stream().filter(t -> t.getStatus() != TaskStatus.DONE).count();
        md.append("- Pending tasks: **").append(pending).append("**\n");
        if (pending > 0) {
            for (Task t : tasks.stream().filter(t -> t.getStatus() != TaskStatus.DONE).limit(15).toList()) {
                md.append("  - ").append(t.getTaskKey()).append(" — ").append(safe(t.getTitle())).append(" (").append(t.getStatus()).append(")\n");
            }
        }
        md.append("\n");

        md.append("## Metrics\n\n");
        md.append("- Total tasks in scope: **").append(tasks.size()).append("**\n");
        md.append("- Completion rate: **");
        md.append(tasks.isEmpty() ? "0" : Math.round(100.0 * done / tasks.size()));
        md.append("%**\n");
        md.append("- Overdue: **").append(tasks.stream().filter(Task::isOverdue).count()).append("**\n\n");

        md.append("## Demo\n\n");
        md.append("_Link demo artefacts (videos, designs, deployed environments) here._\n");

        return md.toString();
    }

    // =====================================================================
    // Retrospective
    // =====================================================================

    private String renderRetrospective(WorkspaceSnapshot snapshot, DocumentationRequest request) {
        String sprintLabel = request.getSprintName() != null
                ? request.getSprintName()
                : (request.getSprintId() != null ? request.getSprintId() : "Current Sprint");

        StringBuilder md = new StringBuilder();
        md.append("# Sprint Retrospective\n\n");
        md.append("**Sprint:** ").append(safe(sprintLabel)).append("\n");
        md.append("**Date:** ").append(today(snapshot)).append("\n");
        md.append("**Facilitator:** ").append(optional(request.getAuthor(), "Scrum Master")).append("\n\n");

        md.append("## What went well\n\n");
        long done = snapshot.getTasks() != null
                ? snapshot.getTasks().stream().filter(t -> t.getStatus() == TaskStatus.DONE).count()
                : 0;
        if (done > 0) {
            md.append("- ").append(done).append(" task(s) completed during this sprint.\n");
        }
        if (snapshot.getMembers() != null && !snapshot.getMembers().isEmpty()) {
            md.append("- Active members contributing: ").append(snapshot.getMembers().size()).append("\n");
        }
        md.append("- Cross-functional collaboration: confirmed.\n\n");

        md.append("## What went poorly\n\n");
        long overdue = snapshot.getTasks() != null
                ? snapshot.getTasks().stream().filter(Task::isOverdue).count()
                : 0;
        if (overdue > 0) {
            md.append("- ").append(overdue).append(" task(s) slipped past their due date.\n");
        }
        if (snapshot.getWorkloads() != null) {
            long overloaded = snapshot.getWorkloads().stream()
                    .filter(w -> "BLOCKED".equalsIgnoreCase(w.getStatus()))
                    .count();
            if (overloaded > 0) {
                md.append("- ").append(overloaded).append(" member(s) flagged as BLOCKED in workload records.\n");
            }
        }
        md.append("- Knowledge silos detected for: _fill in here_.\n\n");

        md.append("## What to improve\n\n");
        md.append("- [ ] Triage overdue tasks within 48h.\n");
        md.append("- [ ] Re-balance workload for overloaded members.\n");
        md.append("- [ ] Document decisions in the workspace pages.\n\n");

        md.append("## Action items for the next sprint\n\n");
        md.append("| # | Action | Owner | Due |\n");
        md.append("|---|--------|-------|-----|\n");
        md.append("| 1 | _Top improvement item_ | _Owner_ | _Date_ |\n");
        md.append("| 2 | _Second improvement item_ | _Owner_ | _Date_ |\n\n");

        md.append("## Team sentiment\n\n");
        md.append("- _Collect emoji votes or 1-5 ratings here before the meeting ends._\n");

        return md.toString();
    }

    // =====================================================================
    // Technical Specification
    // =====================================================================

    private String renderTechnicalSpec(WorkspaceSnapshot snapshot, DocumentationRequest request) {
        Project project = resolveProject(snapshot, request);

        StringBuilder md = new StringBuilder();
        md.append("# Technical Specification\n\n");
        md.append("**Project:** ").append(safe(project != null ? project.getName() : optional(request.getTopic(), "Untitled Project"))).append("\n");
        md.append("**Date:** ").append(today(snapshot)).append("\n");
        md.append("**Audience:** ").append(optional(request.getAudience(), "Engineering Team")).append("\n\n");

        md.append("## 1. Overview\n\n");
        md.append("This document captures the technical architecture, modules, and data model ");
        md.append("for the project. It is generated from the live workspace state and is meant ");
        md.append("to be reviewed and refined by the team before implementation begins.\n\n");

        md.append("## 2. Architecture\n\n");
        md.append("```\n");
        md.append("┌──────────────┐    HTTPS    ┌─────────────────────┐\n");
        md.append("│  Next.js UI  │ ──────────► │  Spring Boot API    │\n");
        md.append("│  (React 18)  │ ◄────────── │  (Java 17)          │\n");
        md.append("└──────────────┘    JSON     └─────────┬───────────┘\n");
        md.append("                                       │ JPA\n");
        md.append("                              ┌────────▼────────┐\n");
        md.append("                              │  PostgreSQL 15  │\n");
        md.append("                              └─────────────────┘\n");
        md.append("                                       │\n");
        md.append("                              ┌────────▼────────┐\n");
        md.append("                              │  Groq AI API    │\n");
        md.append("                              │  (Llama 3.3 70B)│\n");
        md.append("                              └─────────────────┘\n");
        md.append("```\n\n");

        md.append("## 3. Modules\n\n");
        md.append("| Module | Responsibility | Tech |\n");
        md.append("|--------|----------------|------|\n");
        md.append("| `auth` | JWT auth, registration, login | Spring Security |\n");
        md.append("| `workspace` | Workspace + member management | Spring Data JPA |\n");
        md.append("| `project` | Project CRUD, key generation | Spring Data JPA |\n");
        md.append("| `task` | Task CRUD, assignment, status | Spring Data JPA |\n");
        md.append("| `page` | Document pages + blocks | Spring Data JPA |\n");
        md.append("| `ai` | LLM orchestration (Groq) | WebClient |\n");
        md.append("| `dashboard` | Aggregations + SSE updates | Spring Data JPA |\n\n");

        md.append("## 4. Data Model\n\n");
        md.append("- **Workspace** — owns Projects, Pages, Members\n");
        md.append("- **Project** — belongs to Workspace, owns Tasks\n");
        md.append("- **Task** — belongs to Project, has assignee / reporter / labels / checklist\n");
        md.append("- **SubTask** — child of Task\n");
        md.append("- **Page / Block** — document pages with block-based content\n");
        md.append("- **WorkspaceMember** — user ↔ workspace role mapping\n");
        md.append("- **Goal / KeyResult** — OKRs\n");
        md.append("- **ActivityLog** — audit trail\n\n");

        md.append("## 5. APIs\n\n");
        md.append("- `GET  /api/projects/{id}` — fetch project\n");
        md.append("- `POST /api/projects/{id}/tasks` — create task\n");
        md.append("- `POST /api/ai/projects/{id}/analyze` — health analysis\n");
        md.append("- `POST /api/ai/workspace/chat` — workspace Q&A\n");
        md.append("- `POST /api/ai/documentation/{type}` — generate doc\n");
        md.append("- `GET  /api/dashboard/smart/stream` — SSE stream\n\n");

        md.append("## 6. Non-Functional\n\n");
        md.append("- **Latency budget:** 200ms p95 for non-AI endpoints, ≤ 5s p95 for AI endpoints\n");
        md.append("- **Throughput:** 100 req/s sustained per workspace\n");
        md.append("- **Observability:** structured JSON logs, SSE-based live updates\n");
        md.append("- **Security:** JWT, workspace-scoped authorization, rate-limited AI\n\n");

        md.append("## 7. Risks & Mitigations\n\n");
        if (snapshot.getTasks() != null) {
            long overdue = snapshot.getTasks().stream().filter(Task::isOverdue).count();
            if (overdue > 0) {
                md.append("- **Schedule risk:** ").append(overdue).append(" task(s) already overdue.\n");
                md.append("  - _Mitigation:_ reduce WIP, extend deadline, or descope.\n");
            }
        }
        md.append("- **AI vendor risk:** Groq outage or rate limit.\n");
        md.append("  - _Mitigation:_ deterministic local generator as fallback.\n");
        md.append("- **Data integrity:** no schema changes allowed mid-flight.\n");
        md.append("  - _Mitigation:_ feature flags and additive code only.\n\n");

        md.append("## 8. Open Questions\n\n");
        md.append("- [ ] Confirm authentication scheme for embedded pages.\n");
        md.append("- [ ] Confirm retention policy for AI-generated drafts.\n");

        return md.toString();
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private Project resolveProject(WorkspaceSnapshot snapshot, DocumentationRequest request) {
        if (request.getProjectId() != null && snapshot.getProjects() != null) {
            for (Project p : snapshot.getProjects()) {
                if (p.getId() != null && p.getId().toString().equals(request.getProjectId())) {
                    return p;
                }
            }
        }
        // Fall back to most recent project
        if (snapshot.getProjects() != null && !snapshot.getProjects().isEmpty()) {
            return snapshot.getProjects().get(0);
        }
        return null;
    }

    private List<Task> projectTasks(WorkspaceSnapshot snapshot, Project project) {
        if (snapshot.getTasks() == null) return List.of();
        if (project == null) return snapshot.getTasks();
        return snapshot.getTasks().stream()
                .filter(t -> t.getProject() != null && project.getId() != null
                        && project.getId().equals(t.getProject().getId()))
                .toList();
    }

    private String deriveTitle(WorkspaceSnapshot snapshot, DocumentationRequest request) {
        DocumentType type = request.getDocumentType() == null
                ? DocumentType.SRS
                : request.getDocumentType();
        String suffix = switch (type) {
            case SRS -> "Software Requirements Specification";
            case USER_STORIES -> "User Stories";
            case ACCEPTANCE_CRITERIA -> "Acceptance Criteria";
            case MEETING_MINUTES -> "Meeting Minutes";
            case SPRINT_REVIEW -> "Sprint Review";
            case RETROSPECTIVE -> "Sprint Retrospective";
            case TECHNICAL_SPEC -> "Technical Specification";
        };
        Project project = resolveProject(snapshot, request);
        String scope = project != null ? project.getName() : (request.getTopic() != null ? request.getTopic() : "Workspace");
        return scope + " — " + suffix;
    }

    private List<String> extractKeywords(WorkspaceSnapshot snapshot, DocumentType type) {
        List<String> keywords = new ArrayList<>();
        keywords.add(type.name());
        if (snapshot.getWorkspace() != null && snapshot.getWorkspace().getName() != null) {
            keywords.add(snapshot.getWorkspace().getName());
        }
        if (snapshot.getTasks() != null) {
            snapshot.getTasks().stream()
                    .flatMap(t -> t.getLabels().stream())
                    .map(Task.Label::getName)
                    .filter(Objects::nonNull)
                    .distinct()
                    .limit(5)
                    .forEach(keywords::add);
        }
        return keywords;
    }

    private String today(WorkspaceSnapshot snapshot) {
        LocalDate today = snapshot.getToday() != null ? snapshot.getToday() : LocalDate.now();
        return today.format(DATE);
    }

    private String safe(String value) {
        return value != null ? value : "—";
    }

    private String optional(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    private String truncate(String value, int max) {
        if (value == null) return "";
        String collapsed = value.replaceAll("\\s+", " ").trim();
        if (collapsed.length() <= max) return collapsed;
        return collapsed.substring(0, Math.max(0, max - 3)) + "...";
    }

    private List<DocSection> parseOutline(String markdown) {
        List<DocSection> sections = new ArrayList<>();
        if (markdown == null) return sections;
        for (String line : markdown.split("\\R")) {
            if (line.startsWith("# ")) {
                sections.add(DocSection.builder()
                        .level(1)
                        .heading(line.substring(2).trim())
                        .anchor(toAnchor(line.substring(2).trim()))
                        .build());
            } else if (line.startsWith("## ")) {
                sections.add(DocSection.builder()
                        .level(2)
                        .heading(line.substring(3).trim())
                        .anchor(toAnchor(line.substring(3).trim()))
                        .build());
            }
        }
        return sections;
    }

    private String toAnchor(String heading) {
        return heading.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-");
    }
}
