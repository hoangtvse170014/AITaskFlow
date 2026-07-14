package com.taskflow.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.ai.config.GroqConfig;
import com.taskflow.ai.dto.ProjectAnalysisResponse;
import com.taskflow.ai.exception.AiException;
import com.taskflow.ai.exception.AiTimeoutException;
import com.taskflow.entity.*;
import com.taskflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiProjectAnalyzerService {

    private static final String SYSTEM_PROMPT = """
            You are a Senior Agile Project Manager with 15+ years of experience.
            You analyze software projects and provide actionable insights.
            Return ONLY valid JSON, no markdown, no explanation.
            """;

    private static final int MAX_RISKS = 3;
    private static final int MAX_RECOMMENDATIONS = 3;
    private static final int MAX_NEXT_ACTIONS = 3;
    private static final int MAX_RETRIES = 1;

    private final AiService aiService;
    private final GroqConfig groqConfig;
    private final ProjectAnalysisPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final MemberWorkloadRepository workloadRepository;
    private final GoalRepository goalRepository;
    private final ActivityLogRepository activityLogRepository;

    @Transactional(readOnly = true)
    public ProjectAnalysisResponse analyzeProject(UUID projectId) {
        long startTime = System.currentTimeMillis();

        log.info("Starting AI project analysis for project: {}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (project.getWorkspace() == null) {
            throw new IllegalStateException("Project has no associated workspace");
        }

        UUID workspaceId = project.getWorkspace().getId();

        // Load all data INSIDE the read-only transaction, then release the DB
        // connection before calling Groq (which can block up to timeoutSeconds).
        AnalysisContext context = loadAnalysisContext(projectId, workspaceId);

        String prompt = promptBuilder.build(
                context.project(),
                context.tasks(),
                context.workloads(),
                context.goals(),
                context.activities());
        log.debug("Generated prompt for project analysis");

        String aiResponse = callGroqWithRetry(prompt, startTime);

        return parseAndBuildResponse(aiResponse, System.currentTimeMillis() - startTime);
    }

    private record AnalysisContext(
            Project project,
            List<Task> tasks,
            List<MemberWorkload> workloads,
            List<Goal> goals,
            List<ActivityLog> activities) {}

    private AnalysisContext loadAnalysisContext(UUID projectId, UUID workspaceId) {
        return new AnalysisContext(
                projectRepository.findById(projectId).orElseThrow(),
                loadProjectTasks(projectId),
                loadProjectWorkloads(workspaceId),
                loadProjectGoals(workspaceId),
                loadProjectActivities(workspaceId));
    }

    private String callGroqWithRetry(String prompt, long startTime) {
        int attempts = 0;
        String lastError = null;

        while (attempts <= MAX_RETRIES) {
            attempts++;
            try {
                String response = aiService.generate(SYSTEM_PROMPT + "\n\n" + prompt);
                long processingTime = System.currentTimeMillis() - startTime;
                log.info("Groq response received in {}ms (attempt {})", processingTime, attempts);
                return response;
            } catch (AiTimeoutException e) {
                lastError = "Timeout: " + e.getMessage();
                log.warn("Groq request timed out (attempt {}/{}): {}", attempts, MAX_RETRIES + 1, e.getMessage());
                if (attempts > MAX_RETRIES) {
                    throw e;
                }
            } catch (AiException e) {
                lastError = "AI Error: " + e.getMessage();
                log.error("Groq API error (attempt {}/{}): {}", attempts, MAX_RETRIES + 1, e.getMessage());
                if (attempts > MAX_RETRIES) {
                    throw e;
                }
            } catch (Exception e) {
                lastError = "Unexpected: " + e.getMessage();
                log.error("Unexpected error calling Groq (attempt {}/{}): {}", attempts, MAX_RETRIES + 1, e.getMessage());
                if (attempts > MAX_RETRIES) {
                    throw new AiException("Failed to analyze project: " + e.getMessage(), e);
                }
            }

            if (attempts <= MAX_RETRIES) {
                log.info("Retrying Groq request (attempt {}/{})", attempts + 1, MAX_RETRIES + 1);
            }
        }

        throw new AiException("Failed to get response from Groq after " + (MAX_RETRIES + 1) + " attempts. Last error: " + lastError);
    }

    private List<Task> loadProjectTasks(UUID projectId) {
        List<Task> tasks = taskRepository.findAllByProjectIdOrderByPosition(projectId);
        log.debug("Loaded {} tasks for project", tasks.size());
        return tasks;
    }

    private List<MemberWorkload> loadProjectWorkloads(UUID workspaceId) {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);

        List<MemberWorkload> workloads = workloadRepository
                .findByWorkspaceIdAndDateRange(workspaceId, weekAgo, today);

        log.debug("Loaded {} workload records for workspace", workloads.size());
        return workloads;
    }

    private List<Goal> loadProjectGoals(UUID workspaceId) {
        List<Goal> goals = goalRepository.findActiveGoalsByWorkspace(workspaceId);
        log.debug("Loaded {} goals for workspace", goals.size());
        return goals;
    }

    private List<ActivityLog> loadProjectActivities(UUID workspaceId) {
        List<ActivityLog> activities = activityLogRepository
                .findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId);

        if (activities.size() > 20) {
            activities = activities.subList(0, 20);
        }

        log.debug("Loaded {} activities for workspace", activities.size());
        return activities;
    }

    private ProjectAnalysisResponse parseAndBuildResponse(String aiResponse, long processingTime) {
        // SECURITY: never log raw AI response at INFO - it may contain user emails,
        // task titles, and other workspace data. Use DEBUG and gate behind log level.
        if (log.isDebugEnabled()) {
            log.debug("================ RAW RESPONSE ================\n{}\n============================================", aiResponse);
        }

        String cleaned = cleanGroqResponse(aiResponse);

        if (log.isDebugEnabled()) {
            log.debug("================ CLEANED RESPONSE ================\n{}\n=================================================", cleaned);
        }

        if (cleaned == null || cleaned.isBlank()) {
            log.error("Response is empty after cleaning");
            return buildDefaultResponse("Empty response from AI", processingTime);
        }

        if (!isValidJson(cleaned)) {
            log.error("Invalid JSON after cleaning - attempting repair");

            String repaired = repairJson(cleaned);
            if (repaired != null && isValidJson(repaired)) {
                log.info("JSON repaired successfully");
                cleaned = repaired;
            } else {
                log.error("JSON repair failed - will attempt parse anyway");
            }
        }

        try {
            JsonNode root = objectMapper.readTree(cleaned);

            Integer healthScore = root.path("healthScore").asInt(50);
            String summary = root.path("summary").asText("Analysis completed");
            Double confidence = root.path("confidence").asDouble(0.5);

            List<String> risks = parseStringList(root.path("risks"), MAX_RISKS);
            List<String> recommendations = parseStringList(root.path("recommendations"), MAX_RECOMMENDATIONS);
            List<String> nextActions = parseStringList(root.path("nextActions"), MAX_NEXT_ACTIONS);

            ProjectAnalysisResponse response = ProjectAnalysisResponse.success(
                    healthScore,
                    summary,
                    risks,
                    recommendations,
                    nextActions,
                    confidence,
                    processingTime
            );

            log.info("================ PARSED RESPONSE ================");
            log.info("healthScore: {}", response.getHealthScore());
            log.info("summary: {}", response.getSummary());
            log.info("risks count: {}", response.getRisks() != null ? response.getRisks().size() : 0);
            log.info("recommendations count: {}", response.getRecommendations() != null ? response.getRecommendations().size() : 0);
            log.info("nextActions count: {}", response.getNextActions() != null ? response.getNextActions().size() : 0);
            log.info("confidence: {}", response.getConfidence());
            log.info("processingTimeMs: {}", response.getProcessingTimeMs());
            log.info("================================================");

            return response;

        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", e.getMessage());
            log.error("Full cleaned response was:\n{}", cleaned);

            if (cleaned.length() > 2000) {
                log.error("Response preview (first 500 chars):\n{}", cleaned.substring(0, Math.min(500, cleaned.length())));
                log.error("Response preview (last 500 chars):\n{}", cleaned.substring(Math.max(0, cleaned.length() - 500)));
            }

            return buildDefaultResponse("Failed to parse AI response: " + e.getMessage(), processingTime);
        }
    }

    public String cleanGroqResponse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String result = text;

        result = removeBom(result);

        result = result.replaceAll("(?i)```json\\s*", "");
        result = result.replaceAll("(?i)```\\s*", "");

        result = result.trim();

        int firstBrace = result.indexOf('{');
        if (firstBrace >= 0) {
            int matchingClose = findMatchingBrace(result, firstBrace);
            if (matchingClose >= 0) {
                result = result.substring(firstBrace, matchingClose + 1);
            }
        }

        result = result.trim();

        if (result.isEmpty()) {
            return null;
        }

        return result;
    }

    private int findMatchingBrace(String text, int startIndex) {
        if (startIndex < 0 || startIndex >= text.length() || text.charAt(startIndex) != '{') {
            return -1;
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = startIndex; i < text.length(); i++) {
            char c = text.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
        }

        return -1;
    }

    private String removeBom(String text) {
        if (text == null) return null;

        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length >= 3 &&
            bytes[0] == (byte) 0xEF &&
            bytes[1] == (byte) 0xBB &&
            bytes[2] == (byte) 0xBF) {

            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }
        return text;
    }

    private boolean isValidJson(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }

        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            return false;
        }

        int openBraces = 0;
        int closeBraces = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == '{') openBraces++;
                else if (c == '}') closeBraces++;
            }
        }

        if (inString || escaped) {
            return false;
        }

        if (openBraces != closeBraces) {
            log.warn("Mismatched braces: open={}, close={}", openBraces, closeBraces);
            return false;
        }

        try {
            objectMapper.readTree(json);
            return true;
        } catch (Exception e) {
            log.debug("JSON validation failed: {}", e.getMessage());
            return false;
        }
    }

    private String repairJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }

        String repaired = json.trim();

        repaired = repaired.replaceAll(",\\s*]", "]");
        repaired = repaired.replaceAll(",\\s*}", "}");

        repaired = repaired.replaceAll("([{,])\\s*'", "$1\"").replaceAll("'\\s*:", "\":");
        repaired = repaired.replaceAll(":\\s*'([^']*)'\\s*([,}])", ":\"$1\"$2");

        int lastValidPos = repaired.length();
        int braceCount = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < repaired.length(); i++) {
            char c = repaired.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == '{') braceCount++;
                else if (c == '}') braceCount--;

                if (braceCount < 0) {
                    lastValidPos = i;
                    break;
                }
            }
        }

        if (braceCount > 0 && lastValidPos > 0) {
            int closePos = repaired.indexOf('}', lastValidPos);
            if (closePos > 0) {
                repaired = repaired.substring(0, closePos + 1);
            }
        }

        repaired = repaired.replaceAll(",\\s*}", "}");

        try {
            objectMapper.readTree(repaired);
            log.info("JSON repair successful");
            return repaired;
        } catch (Exception e) {
            log.warn("JSON repair failed: {}", e.getMessage());
            return null;
        }
    }

    private List<String> parseStringList(JsonNode node, int maxItems) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            int count = 0;
            for (JsonNode item : node) {
                if (count >= maxItems) break;
                String text = item.asText();
                if (text != null && !text.isBlank()) {
                    list.add(text);
                    count++;
                }
            }
        }
        return list;
    }

    private ProjectAnalysisResponse buildDefaultResponse(String errorMessage, long processingTime) {
        log.warn("Building default response due to: {}", errorMessage);
        return ProjectAnalysisResponse.success(
                50,
                "AI analysis temporarily unavailable. " + errorMessage,
                List.of("Unable to generate risk analysis"),
                List.of("Review project metrics manually"),
                List.of("Check AI service configuration"),
                0.0,
                processingTime
        );
    }
}
