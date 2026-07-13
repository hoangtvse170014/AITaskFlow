package com.taskflow.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.ai.config.GeminiConfig;
import com.taskflow.ai.dto.WorkspaceAnswerResponse;
import com.taskflow.ai.dto.WorkspaceAnswerResponse.RelatedItem;
import com.taskflow.ai.exception.AiException;
import com.taskflow.ai.exception.AiTimeoutException;
import com.taskflow.entity.*;
import com.taskflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceAssistantService {

    private static final String SYSTEM_PROMPT = """
            You are a Senior Agile Coach, Technical Lead, and Project Manager.
            Answer questions using ONLY the provided workspace data.
            If information is unavailable, say: "I don't have enough workspace information to answer this question."
            NEVER hallucinate or make up information.
            Return ONLY valid JSON, no markdown, no explanation.
            """;

    private final AiService aiService;
    private final GeminiConfig geminiConfig;
    private final WorkspaceRepository workspaceRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final GoalRepository goalRepository;
    private final PageRepository pageRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final MemberWorkloadRepository workloadRepository;
    private final ActivityLogRepository activityLogRepository;
    private final WorkspaceContextBuilder contextBuilder;
    private final ObjectMapper objectMapper;

    public WorkspaceAnswerResponse answerQuestion(UUID workspaceId, String question) {
        long startTime = System.currentTimeMillis();

        log.info("Processing workspace AI question for workspace: {}", workspaceId);

        // Phase 1: load all workspace data inside the transaction, then close it
        // before calling Gemini (the call may block up to timeoutSeconds).
        WorkspaceContext ctx = loadWorkspaceContextTx(workspaceId, question);

        String aiResponse = callGemini(ctx.context());
        long processingTime = System.currentTimeMillis() - startTime;

        return parseResponse(aiResponse, processingTime, ctx.workspace(), ctx.projects(), ctx.tasks());
    }

    private record WorkspaceContext(
            String context,
            Workspace workspace,
            List<Project> projects,
            List<Task> tasks) {}

    @Transactional(readOnly = true)
    protected WorkspaceContext loadWorkspaceContextTx(UUID workspaceId, String question) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        List<Project> projects = loadProjects(workspaceId);
        List<Task> tasks = loadTasks(workspaceId);
        List<Goal> goals = loadGoals(workspaceId);
        List<Page> pages = loadPages(workspaceId);
        List<WorkspaceMember> members = loadMembers(workspaceId);
        List<MemberWorkload> workloads = loadWorkloads(workspaceId);
        List<ActivityLog> activities = loadActivities(workspaceId);

        String context = contextBuilder.buildContext(
                workspace, projects, tasks, goals, pages, members, workloads, activities, question);

        return new WorkspaceContext(context, workspace, projects, tasks);
    }

    private List<Project> loadProjects(UUID workspaceId) {
        List<Project> projects = projectRepository.findAllByWorkspaceId(workspaceId);
        log.debug("Loaded {} projects", projects.size());
        return projects;
    }

    private List<Task> loadTasks(UUID workspaceId) {
        List<Task> tasks = taskRepository.findAllByWorkspaceIdOrderByUpdatedAtDesc(workspaceId);
        log.debug("Loaded {} tasks", tasks.size());
        return tasks;
    }

    private List<Goal> loadGoals(UUID workspaceId) {
        List<Goal> goals = goalRepository.findActiveGoalsByWorkspace(workspaceId);
        if (goals.isEmpty()) {
            goals = goalRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        }
        log.debug("Loaded {} goals", goals.size());
        return goals;
    }

    private List<Page> loadPages(UUID workspaceId) {
        List<Page> pages = pageRepository.findAllActivePagesByWorkspace(workspaceId);
        if (pages.isEmpty()) {
            pages = pageRepository.findAllByWorkspaceIdOrderBySidebarOrder(workspaceId);
        }
        log.debug("Loaded {} pages", pages.size());
        return pages;
    }

    private List<WorkspaceMember> loadMembers(UUID workspaceId) {
        List<WorkspaceMember> members = memberRepository.findAllActiveByWorkspaceId(workspaceId);
        log.debug("Loaded {} members", members.size());
        return members;
    }

    private List<MemberWorkload> loadWorkloads(UUID workspaceId) {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);
        List<MemberWorkload> workloads = workloadRepository.findByWorkspaceIdAndDateRange(workspaceId, weekAgo, today);
        log.debug("Loaded {} workload records", workloads.size());
        return workloads;
    }

    private List<ActivityLog> loadActivities(UUID workspaceId) {
        List<ActivityLog> activities = activityLogRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        if (activities.size() > 30) {
            activities = activities.subList(0, 30);
        }
        log.debug("Loaded {} activities", activities.size());
        return activities;
    }

    private String callGemini(String prompt) {
        if (!geminiConfig.isValid()) {
            throw new AiException("Gemini API key is not configured or invalid");
        }

        try {
            return aiService.generate(SYSTEM_PROMPT + "\n\n" + prompt);
        } catch (AiTimeoutException e) {
            log.error("Gemini request timed out during workspace assistant", e);
            throw e;
        } catch (AiException e) {
            log.error("Gemini API error during workspace assistant: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during workspace assistant", e);
            throw new AiException("Failed to process workspace question: " + e.getMessage(), e);
        }
    }

    private WorkspaceAnswerResponse parseResponse(String aiResponse, long processingTime,
                                                   Workspace workspace, List<Project> projects, List<Task> tasks) {
        try {
            String json = extractJson(aiResponse);

            if (json == null || json.isBlank()) {
                return buildErrorResponse("Empty response from AI", processingTime);
            }

            JsonNode root = objectMapper.readTree(json);

            String answer = root.path("answer").asText("I don't have enough workspace information to answer this question.");
            Double confidence = root.path("confidence").asDouble(0.0);
            List<String> sources = parseStringList(root.path("sources"));
            List<RelatedItem> relatedProjects = parseRelatedProjects(root.path("relatedProjects"));
            List<RelatedItem> relatedTasks = parseRelatedTasks(root.path("relatedTasks"));
            List<String> suggestions = parseStringList(root.path("suggestions"));

            if (answer == null || answer.isBlank()) {
                return buildErrorResponse("No answer provided by AI", processingTime);
            }

            return WorkspaceAnswerResponse.success(
                    answer,
                    sources,
                    confidence,
                    relatedProjects,
                    relatedTasks,
                    suggestions,
                    processingTime
            );

        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse workspace assistant response: {}", e.getMessage());
            return buildErrorResponse("Failed to parse AI response: " + e.getMessage(), processingTime);
        }
    }

    private List<String> parseStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                String text = item.asText();
                if (text != null && !text.isBlank()) {
                    list.add(text);
                }
            }
        }
        return list;
    }

    private List<RelatedItem> parseRelatedProjects(JsonNode node) {
        List<RelatedItem> list = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                RelatedItem ri = RelatedItem.builder()
                        .id(item.path("id").asText(null))
                        .name(item.path("name").asText(null))
                        .type("project")
                        .status(item.path("status").asText(null))
                        .description(item.path("description").asText(null))
                        .build();
                if (ri.getName() != null) {
                    list.add(ri);
                }
            }
        }
        return list;
    }

    private List<RelatedItem> parseRelatedTasks(JsonNode node) {
        List<RelatedItem> list = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                RelatedItem ri = RelatedItem.builder()
                        .id(item.path("id").asText(null))
                        .name(item.path("name").asText(null))
                        .type("task")
                        .status(item.path("status").asText(null))
                        .description(item.path("description").asText(null))
                        .build();
                if (ri.getName() != null) {
                    list.add(ri);
                }
            }
        }
        return list;
    }

    private String extractJson(String text) {
        if (text == null) {
            return null;
        }

        String trimmed = text.trim();

        int jsonStart = trimmed.indexOf('{');
        int jsonEnd = trimmed.lastIndexOf('}');

        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return trimmed.substring(jsonStart, jsonEnd + 1);
        }

        return trimmed;
    }

    private WorkspaceAnswerResponse buildErrorResponse(String errorMessage, long processingTime) {
        return WorkspaceAnswerResponse.success(
                "I encountered an issue while processing your question. " + errorMessage,
                List.of("error"),
                0.0,
                null,
                null,
                List.of("Try rephrasing your question or check if the workspace has data."),
                processingTime
        );
    }
}
