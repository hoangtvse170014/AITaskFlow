package com.taskflow.ai.service;

import com.taskflow.ai.config.GroqConfig;
import com.taskflow.ai.dto.WorkspaceAnswerResponse;
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
 * Orchestrates a workspace assistant question through the following pipeline:
 *
 * <ol>
 *   <li>Classify intent (rule-based, no LLM call)</li>
 *   <li>Load workspace snapshot from database (inside a read-only transaction)</li>
 *   <li>Build structured context from snapshot</li>
 *   <li>Route to Groq (with intent-specific system prompt) or fall back to
 *       {@link WorkspaceAssistantLocalAnswerer} if Groq is unavailable</li>
 *   <li>Parse response into {@link WorkspaceAnswerResponse}</li>
 * </ol>
 *
 * <p>The local answerer is always available and never hallucinates - every number
 * and name in its output is derived deterministically from the database snapshot.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceAssistantService {

    private final QuestionIntentClassifier intentClassifier;
    private final WorkspaceContextBuilder contextBuilder;
    private final IntentPromptFactory promptFactory;
    private final WorkspaceAssistantLocalAnswerer localAnswerer;
    private final AiService aiService;
    private final GroqConfig groqConfig;
    // Repositories
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

    public WorkspaceAnswerResponse answerQuestion(UUID workspaceId, String question) {
        long startTime = System.currentTimeMillis();

        // Step 1: classify intent before touching the DB
        QuestionIntentClassifier.Intent intent = intentClassifier.classify(question);
        log.info("Workspace AI intent classified: {} for workspace={}", intent, workspaceId);

        // Step 2: load snapshot inside a read-only transaction (closes DB connection before LLM call)
        WorkspaceSnapshot snapshot = loadSnapshot(workspaceId);

        // Step 3: build text context
        String context = contextBuilder.buildContext(snapshot, question);

        // Step 4: call LLM or fall back to local answerer
        WorkspaceAnswerResponse response;
        if (groqConfig.isValid()) {
            response = callGroqWithIntent(intent, context, question, snapshot, startTime);
        } else {
            log.info("Groq not configured - using local answerer");
            response = localAnswerer.answer(snapshot, question, intent,
                    System.currentTimeMillis() - startTime);
        }

        return response;
    }

    @Transactional(readOnly = true)
    protected WorkspaceSnapshot loadSnapshot(UUID workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        List<Project> projects = projectRepository.findAllByWorkspaceId(workspaceId);
        // Use the eager query so we get assignees without N+1
        List<Task> tasks = taskRepository.findAllByWorkspaceIdEager(workspaceId);
        List<SubTask> subtasks = subTaskRepository.findAllByWorkspaceId(workspaceId);
        List<Goal> goals = loadGoals(workspaceId);
        List<Page> pages = loadPages(workspaceId);
        List<Block> recentBlocks = blockRepository.findRecentTextBlocksByWorkspaceId(workspaceId);
        List<WorkspaceMember> members = memberRepository.findAllActiveByWorkspaceId(workspaceId);
        List<MemberWorkload> workloads = loadWorkloads(workspaceId);
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

    private WorkspaceAnswerResponse callGroqWithIntent(
            QuestionIntentClassifier.Intent intent,
            String context,
            String question,
            WorkspaceSnapshot snapshot,
            long startTime) {

        String systemPrompt = promptFactory.buildSystemPrompt(intent, snapshot);
        String userPrompt = "WORKSPACE AI CONTEXT:\n" + context + "\n\nUSER QUESTION:\n" + question;

        try {
            String rawResponse = aiService.generateWithSystemPrompt(systemPrompt, userPrompt);
            long elapsed = System.currentTimeMillis() - startTime;

            WorkspaceAnswerResponse parsed = localAnswerer.parseLlmEnvelope(rawResponse, intent, elapsed);
            // Enrich with local suggestions if LLM returned none
            if (parsed.getSuggestions() == null || parsed.getSuggestions().isEmpty()) {
                parsed.setSuggestions(List.of(
                        "Tóm tắt workspace",
                        "Dự án rủi ro nhất",
                        "Thành viên quá tải",
                        "Lên sprint"));
            }
            return parsed;

        } catch (AiTimeoutException e) {
            log.warn("Groq timed out during workspace assistant, falling back to local answerer", e);
            return localAnswerer.answer(snapshot, question, intent,
                    System.currentTimeMillis() - startTime);
        } catch (AiException e) {
            log.error("Groq API error during workspace assistant: {}", e.getMessage(), e);
            return localAnswerer.answer(snapshot, question, intent,
                    System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("Unexpected error during workspace assistant", e);
            throw new AiException("Failed to process workspace question: " + e.getMessage(), e);
        }
    }

    // =====================================================================
    // Private helpers for loading individual entity lists
    // =====================================================================

    private List<Goal> loadGoals(UUID workspaceId) {
        List<Goal> goals = goalRepository.findActiveGoalsByWorkspace(workspaceId);
        if (goals.isEmpty()) {
            goals = goalRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        }
        return goals;
    }

    private List<Page> loadPages(UUID workspaceId) {
        List<Page> pages = pageRepository.findAllActivePagesByWorkspace(workspaceId);
        if (pages.isEmpty()) {
            pages = pageRepository.findAllByWorkspaceIdOrderBySidebarOrder(workspaceId);
        }
        return pages;
    }

    private List<MemberWorkload> loadWorkloads(UUID workspaceId) {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);
        return workloadRepository.findByWorkspaceIdAndDateRange(workspaceId, weekAgo, today);
    }
}
