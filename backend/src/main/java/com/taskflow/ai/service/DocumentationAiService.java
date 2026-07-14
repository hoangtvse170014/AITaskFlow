package com.taskflow.ai.service;

import com.taskflow.ai.config.GroqConfig;
import com.taskflow.ai.dto.DocSection;
import com.taskflow.ai.dto.DocumentType;
import com.taskflow.ai.dto.DocumentationRequest;
import com.taskflow.ai.dto.DocumentationResponse;
import com.taskflow.ai.dto.WorkspaceSnapshot;
import com.taskflow.ai.exception.AiException;
import com.taskflow.ai.exception.AiTimeoutException;
import com.taskflow.entity.*;
import com.taskflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates AI Documentation generation.
 *
 * <p>The pipeline is:
 * <ol>
 *   <li>Validate request (workspace + document type are required).</li>
 *   <li>Load the workspace snapshot inside a read-only transaction so the
 *       blocking LLM call never holds a DB connection.</li>
 *   <li>Build a focused textual context for the requested document type.</li>
 *   <li>If Groq is configured, build the type-specific prompt, call the LLM,
 *       and post-process the Markdown. If the call fails or times out, fall
 *       back to the deterministic {@link DocumentationLocalGenerator}.</li>
 *   <li>If Groq is not configured (demo mode), call the local generator
 *       directly.</li>
 * </ol>
 *
 * <p>No entity is ever persisted as a side-effect of this service. The
 * "no database modification" rule is enforced by the fact that we only call
 * read-only repositories and never touch a write repository.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentationAiService {

    private final WorkspaceContextBuilder contextBuilder;
    private final DocumentationPromptFactory promptFactory;
    private final DocumentationLocalGenerator localGenerator;
    private final AiService aiService;
    private final GroqConfig groqConfig;

    // Repositories — all read-only
    private final WorkspaceRepository workspaceRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final SubTaskRepository subTaskRepository;
    private final GoalRepository goalRepository;
    private final PageRepository pageRepository;
    private final BlockRepository blockRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final MemberWorkloadRepository workloadRepository;
    private final ActivityLogRepository activityLogRepository;
    private final TaskCommentRepository commentRepository;

    public DocumentationResponse generate(DocumentationRequest request) {
        long startTime = System.currentTimeMillis();
        DocumentType type = request.getDocumentType();
        if (type == null) {
            return DocumentationResponse.error(null, "documentType is required");
        }
        UUID workspaceId;
        try {
            workspaceId = UUID.fromString(request.getWorkspaceId());
        } catch (IllegalArgumentException e) {
            return DocumentationResponse.error(type, "workspaceId is invalid");
        }

        WorkspaceSnapshot snapshot = loadSnapshot(workspaceId);

        String context = contextBuilder.buildContext(snapshot, request.getTopic());

        DocumentationResponse response;
        if (groqConfig.isValid()) {
            try {
                response = callGroq(type, request, context, startTime);
                if (response.getMarkdown() == null || response.getMarkdown().isBlank()) {
                    log.warn("Groq returned empty Markdown for {}, falling back to local generator", type);
                    response = localGenerator.generate(snapshot, request);
                }
            } catch (Exception ex) {
                log.warn("Groq call failed ({}), falling back to local generator: {}", type, ex.getMessage());
                response = localGenerator.generate(snapshot, request);
            }
        } else {
            log.info("Groq not configured - using local generator for {}", type);
            response = localGenerator.generate(snapshot, request);
        }

        // Enrich outline + keywords from the final markdown (if Groq path).
        if (response.getSections() == null || response.getSections().isEmpty()) {
            response.setSections(parseOutline(response.getMarkdown()));
        }

        return response;
    }

    @Transactional(readOnly = true)
    protected WorkspaceSnapshot loadSnapshot(UUID workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        List<Project> projects = projectRepository.findAllByWorkspaceId(workspaceId);
        List<Task> tasks = taskRepository.findAllByWorkspaceIdEager(workspaceId);
        List<SubTask> subtasks = subTaskRepository.findAllByWorkspaceId(workspaceId);
        List<Goal> goals = goalRepository.findActiveGoalsByWorkspace(workspaceId);
        if (goals.isEmpty()) {
            goals = goalRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        }
        List<Page> pages = pageRepository.findAllActivePagesByWorkspace(workspaceId);
        if (pages.isEmpty()) {
            pages = pageRepository.findAllByWorkspaceIdOrderBySidebarOrder(workspaceId);
        }
        List<Block> recentBlocks = blockRepository.findRecentTextBlocksByWorkspaceId(workspaceId);
        List<WorkspaceMember> members = memberRepository.findAllActiveByWorkspaceId(workspaceId);
        List<MemberWorkload> workloads = workloadRepository.findByWorkspaceIdAndDateRange(
                workspaceId, LocalDate.now().minusDays(7), LocalDate.now());
        List<ActivityLog> activities = activityLogRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        List<TaskComment> comments = commentRepository.findRecentByWorkspaceId(workspaceId);

        return WorkspaceSnapshot.builder()
                .workspace(workspace)
                .projects(projects)
                .tasks(tasks)
                .subtasks(subtasks)
                .goals(goals)
                .pages(pages)
                .recentBlocks(recentBlocks)
                .members(members)
                .workloads(workloads)
                .recentActivities(activities)
                .recentComments(comments)
                .today(LocalDate.now())
                .build();
    }

    private DocumentationResponse callGroq(DocumentType type,
                                            DocumentationRequest request,
                                            String context,
                                            long startTime) {
        String systemPrompt = promptFactory.buildSystemPrompt(type);
        String userPrompt = promptFactory.buildUserPrompt(type, request, context);

        try {
            String raw = aiService.generateWithSystemPrompt(systemPrompt, userPrompt);
            String markdown = stripCodeFences(raw);
            long elapsed = System.currentTimeMillis() - startTime;

            return DocumentationResponse.builder()
                    .documentType(type)
                    .markdown(markdown)
                    .source("GROQ")
                    .confidence(0.8)
                    .processingTimeMs(elapsed)
                    .build();
        } catch (AiTimeoutException e) {
            log.warn("Groq timed out during documentation generation ({}) — falling back to local", type);
            throw e; // let the caller decide between hard-fail and fallback
        } catch (AiException e) {
            log.error("Groq API error during documentation generation ({}): {}", type, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during documentation generation ({})", type, e);
            throw new AiException("Failed to generate documentation: " + e.getMessage(), e);
        }
    }

    private String stripCodeFences(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.startsWith("```markdown")) {
            trimmed = trimmed.substring("```markdown".length());
        } else if (trimmed.startsWith("```md")) {
            trimmed = trimmed.substring("```md".length());
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    private List<DocSection> parseOutline(String markdown) {
        java.util.List<DocSection> sections = new java.util.ArrayList<>();
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
        return heading.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-");
    }
}
