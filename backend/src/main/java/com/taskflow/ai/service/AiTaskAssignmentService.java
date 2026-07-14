package com.taskflow.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.ai.config.GroqConfig;
import com.taskflow.ai.dto.BatchTaskAssignmentRequest;
import com.taskflow.ai.dto.BatchTaskAssignmentResponse;
import com.taskflow.ai.dto.TaskAssignmentRequest;
import com.taskflow.ai.dto.TaskAssignmentResponse;
import com.taskflow.ai.exception.AiException;
import com.taskflow.entity.*;
import com.taskflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiTaskAssignmentService {

    private static final String SYSTEM_PROMPT = """
            You are a Senior Engineering Manager with deep expertise in:
            - Task allocation and workload balancing
            - Skills assessment and team composition
            - Agile project management

            Analyze team data objectively and recommend the best assignee based on:
            1. Current workload percentage
            2. Task count (open and in-progress)
            3. Recent completion rate
            4. Role/skills match
            5. Availability and deadlines

            Return ONLY valid JSON, no explanation or markdown.
            """;

    private final AiService aiService;
    private final GroqConfig groqConfig;
    private final TaskAssignmentPromptBuilder promptBuilder;
    private final BatchTaskAssignmentPromptBuilder batchPromptBuilder;
    private final SkillResolver skillResolver;
    private final ObjectMapper objectMapper;
    private final ProjectRepository projectRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final MemberWorkloadRepository workloadRepository;
    private final TaskRepository taskRepository;
    private final ActivityLogRepository activityLogRepository;

    @Transactional(readOnly = true)
    public TaskAssignmentResponse recommend(TaskAssignmentRequest request) {
        long startTime = System.currentTimeMillis();

        log.info("Starting AI task assignment recommendation for task: {}", request.getTitle());

        // Phase 1: load all data inside the transaction, then close it before
        // calling Groq so we don't hold a DB connection during the blocking HTTP call.
        AssignmentContext ctx = loadContext(request);

        if (ctx.rankedMembers().isEmpty()) {
            return TaskAssignmentResponse.error("No members found in workspace");
        }

        String prompt = promptBuilder.build(ctx.taskData(), ctx.memberDataList(), ctx.project());
        log.debug("Generated prompt for task assignment");

        String aiResponse = callGroq(prompt);
        long processingTime = System.currentTimeMillis() - startTime;

        return parseAndBuildResponse(aiResponse, ctx.memberDataList(), processingTime);
    }

    @Transactional(readOnly = true)
    public BatchTaskAssignmentResponse batchRecommend(BatchTaskAssignmentRequest request) {
        return batchRecommendInternal(request, false);
    }

    /**
     * Variant used by preview flows where the project does not yet exist in the database.
     * Pass {@code previewMode = true} to skip project lookup and use a synthetic project name
     * in the prompt.
     */
    @Transactional(readOnly = true)
    public BatchTaskAssignmentResponse batchRecommendPreview(BatchTaskAssignmentRequest request, String fallbackProjectName) {
        return batchRecommendInternal(request, true, fallbackProjectName);
    }

    private BatchTaskAssignmentResponse batchRecommendInternal(BatchTaskAssignmentRequest request, boolean previewMode) {
        return batchRecommendInternal(request, previewMode, null);
    }

    private BatchTaskAssignmentResponse batchRecommendInternal(
            BatchTaskAssignmentRequest request,
            boolean previewMode,
            String fallbackProjectName) {

        long startTime = System.currentTimeMillis();

        log.info("Starting BATCH AI task assignment for project={}, tasks={}, previewMode={}",
                request.getProjectId(), request.getTasks().size(), previewMode);

        Project project;
        if (previewMode) {
            project = Project.builder()
                    .id(request.getProjectId())
                    .name(fallbackProjectName != null ? fallbackProjectName : "Preview Project")
                    .description("Preview project (not yet persisted)")
                    .build();
        } else {
            project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new IllegalArgumentException("Project not found: " + request.getProjectId()));
        }

        List<WorkspaceMember> members = loadWorkspaceMembers(request.getWorkspaceId());
        List<MemberWorkload> workloads = loadWorkloads(request.getWorkspaceId());
        List<WorkspaceMember> rankedMembers = buildMemberRanking(members, workloads);

        List<TaskAssignmentPromptBuilder.MemberData> memberDataList = rankedMembers.stream()
                .map(member -> buildMemberData(member, workloads))
                .collect(Collectors.toList());

        if (memberDataList.isEmpty()) {
            return BatchTaskAssignmentResponse.error("No members found in workspace");
        }

        String prompt = batchPromptBuilder.build(project, request.getTasks(), memberDataList);
        log.debug("Generated BATCH assignment prompt ({} tasks, {} members)",
                request.getTasks().size(), memberDataList.size());

        String aiResponse = callGroq(prompt);
        long processingTime = System.currentTimeMillis() - startTime;

        return parseBatchResponse(aiResponse, project, memberDataList, processingTime);
    }

    private BatchTaskAssignmentResponse parseBatchResponse(
            String aiResponse,
            Project project,
            List<TaskAssignmentPromptBuilder.MemberData> memberDataList,
            long processingTime) {

        try {
            String json = extractJson(aiResponse);
            if (json == null || json.isBlank()) {
                return buildFallbackBatchResponse(project, request -> request, memberDataList,
                        "Empty AI response", processingTime);
            }

            JsonNode root = objectMapper.readTree(json);

            List<BatchTaskAssignmentResponse.TaskAssignment> assignments = parseAssignments(
                    root.path("assignments"), memberDataList);

            List<BatchTaskAssignmentResponse.WorkloadSummary> summary = parseWorkloadSummary(
                    root.path("workloadSummary"), memberDataList);

            Double overall = root.path("overallConfidence").asDouble(0.0);
            List<String> warnings = parseStringList(root.path("warnings"));
            String reason = root.path("reason").asText("Batch assignment completed");

            return BatchTaskAssignmentResponse.builder()
                    .projectName(project.getName())
                    .assignments(assignments)
                    .workloadSummary(summary)
                    .overallConfidence(overall)
                    .warnings(warnings)
                    .reason(reason)
                    .processingTimeMs(processingTime)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse BATCH AI response: {}", e.getMessage());
            return buildFallbackBatchResponse(project, request -> request, memberDataList,
                    "Failed to parse AI response: " + e.getMessage(), processingTime);
        }
    }

    private List<BatchTaskAssignmentResponse.TaskAssignment> parseAssignments(
            JsonNode assignmentsNode,
            List<TaskAssignmentPromptBuilder.MemberData> memberDataList) {

        List<BatchTaskAssignmentResponse.TaskAssignment> out = new ArrayList<>();
        if (!assignmentsNode.isArray()) return out;

        Map<String, TaskAssignmentPromptBuilder.MemberData> memberById = memberDataList.stream()
                .collect(Collectors.toMap(m -> m.getId(), m -> m, (a, b) -> a));

        for (JsonNode item : assignmentsNode) {
            String memberId = item.path("assignedMemberId").asText(null);
            TaskAssignmentPromptBuilder.MemberData matched = null;
            if (memberId != null) {
                matched = memberById.get(memberId);
            }

            BatchTaskAssignmentResponse.TaskAssignment a = BatchTaskAssignmentResponse.TaskAssignment.builder()
                    .taskRef(item.path("taskRef").asText(null))
                    .title(item.path("title").asText(""))
                    .assignedMemberId(matched != null ? matched.getId() : (memberId != null ? memberId : null))
                    .assignedMemberName(matched != null ? matched.getName() : item.path("assignedMemberName").asText(null))
                    .roleMatched(item.path("roleMatched").asText(null))
                    .confidence(item.path("confidence").asDouble(0.0))
                    .reason(item.path("reason").asText(""))
                    .unassigned(memberId == null || memberId.isBlank() || "null".equalsIgnoreCase(memberId))
                    .build();

            out.add(a);
        }
        return out;
    }

    private List<BatchTaskAssignmentResponse.WorkloadSummary> parseWorkloadSummary(
            JsonNode node,
            List<TaskAssignmentPromptBuilder.MemberData> memberDataList) {

        List<BatchTaskAssignmentResponse.WorkloadSummary> out = new ArrayList<>();
        if (!node.isArray()) return out;

        Map<String, TaskAssignmentPromptBuilder.MemberData> memberById = memberDataList.stream()
                .collect(Collectors.toMap(m -> m.getId(), m -> m, (a, b) -> a));

        for (JsonNode item : node) {
            String id = item.path("memberId").asText(null);
            TaskAssignmentPromptBuilder.MemberData m = id != null ? memberById.get(id) : null;
            out.add(BatchTaskAssignmentResponse.WorkloadSummary.builder()
                    .memberId(id)
                    .memberName(item.path("memberName").asText(m != null ? m.getName() : ""))
                    .assignedTaskCount(item.path("assignedTaskCount").asInt(0))
                    .currentWorkloadPercent(m != null ? m.getWorkloadPercentage() : 0.0)
                    .estimatedNewWorkloadPercent(item.path("estimatedNewWorkloadPercent").asDouble(0.0))
                    .build());
        }
        return out;
    }

    private BatchTaskAssignmentResponse buildFallbackBatchResponse(
            Project project,
            java.util.function.Function<Object, Object> ignored,
            List<TaskAssignmentPromptBuilder.MemberData> memberDataList,
            String errorMessage,
            long processingTime) {

        log.warn("Falling back to deterministic batch assignment: {}", errorMessage);

        // Deterministic fallback: route each task to the least-loaded qualified member.
        List<BatchTaskAssignmentResponse.TaskAssignment> fallback = new ArrayList<>();
        Map<String, Integer> assignedCount = new HashMap<>();
        Map<String, Double> load = new HashMap<>();
        for (TaskAssignmentPromptBuilder.MemberData m : memberDataList) {
            load.put(m.getId(), m.getWorkloadPercentage());
            assignedCount.put(m.getId(), 0);
        }

        // (We don't have the original task inputs here in the fallback path,
        // so we just emit a placeholder explaining the fallback.)
        BatchTaskAssignmentResponse.TaskAssignment placeholder = BatchTaskAssignmentResponse.TaskAssignment.builder()
                .taskRef("__fallback__")
                .title("AI analysis failed; please retry")
                .assignedMemberId(null)
                .assignedMemberName(null)
                .roleMatched(null)
                .confidence(0.0)
                .reason("AI analysis unavailable: " + errorMessage)
                .unassigned(true)
                .build();
        fallback.add(placeholder);

        return BatchTaskAssignmentResponse.builder()
                .projectName(project.getName())
                .assignments(fallback)
                .workloadSummary(List.of())
                .overallConfidence(0.0)
                .warnings(List.of("AI analysis failed - manual assignment required", errorMessage))
                .reason("Fallback: " + errorMessage)
                .processingTimeMs(processingTime)
                .build();
    }

    private record AssignmentContext(
            Project project,
            List<TaskAssignmentPromptBuilder.MemberData> memberDataList,
            TaskAssignmentPromptBuilder.TaskAssignmentData taskData,
            List<WorkspaceMember> rankedMembers) {}

    private AssignmentContext loadContext(TaskAssignmentRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + request.getProjectId()));

        List<WorkspaceMember> members = loadWorkspaceMembers(request.getWorkspaceId());
        List<MemberWorkload> workloads = loadWorkloads(request.getWorkspaceId());
        List<WorkspaceMember> rankedMembers = buildMemberRanking(members, workloads);

        TaskAssignmentPromptBuilder.TaskAssignmentData taskData = buildTaskData(request, project);
        List<TaskAssignmentPromptBuilder.MemberData> memberDataList = rankedMembers.stream()
                .map(member -> buildMemberData(member, workloads))
                .collect(Collectors.toList());

        return new AssignmentContext(project, memberDataList, taskData, rankedMembers);
    }

    private List<WorkspaceMember> loadWorkspaceMembers(UUID workspaceId) {
        List<WorkspaceMember> members = memberRepository.findAllActiveByWorkspaceId(workspaceId);
        log.debug("Loaded {} active members for workspace", members.size());
        return members;
    }

    private List<MemberWorkload> loadWorkloads(UUID workspaceId) {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);

        List<MemberWorkload> workloads = workloadRepository
                .findByWorkspaceIdAndDateRange(workspaceId, weekAgo, today);

        log.debug("Loaded {} workload records for workspace", workloads.size());
        return workloads;
    }

    private List<WorkspaceMember> buildMemberRanking(List<WorkspaceMember> members, List<MemberWorkload> workloads) {
        return members.stream()
                .sorted((m1, m2) -> {
                    double w1 = getAverageWorkload(m1.getId(), workloads);
                    double w2 = getAverageWorkload(m2.getId(), workloads);
                    return Double.compare(w1, w2);
                })
                .collect(Collectors.toList());
    }

    private double getAverageWorkload(UUID memberId, List<MemberWorkload> workloads) {
        return workloads.stream()
                .filter(w -> w.getMember().getId().equals(memberId))
                .mapToInt(w -> w.getWorkloadPercentage() != null ? w.getWorkloadPercentage() : 0)
                .average()
                .orElse(0);
    }

    private TaskAssignmentPromptBuilder.TaskAssignmentData buildTaskData(TaskAssignmentRequest request, Project project) {
        return new TaskAssignmentPromptBuilder.TaskAssignmentData(
                request.getTitle(),
                request.getDescription(),
                request.getPriority(),
                request.getDueDate(),
                project.getName()
        );
    }

    private TaskAssignmentPromptBuilder.MemberData buildMemberData(WorkspaceMember member, List<MemberWorkload> workloads) {
        List<MemberWorkload> memberWorkloads = workloads.stream()
                .filter(w -> w.getMember().getId().equals(member.getId()))
                .toList();

        double avgWorkload = memberWorkloads.stream()
                .mapToInt(w -> w.getWorkloadPercentage() != null ? w.getWorkloadPercentage() : 0)
                .average()
                .orElse(0);

        int openTasks = memberWorkloads.stream()
                .mapToInt(w -> w.getOpenTasks() != null ? w.getOpenTasks() : 0)
                .max()
                .orElse(0);

        int inProgressTasks = memberWorkloads.stream()
                .mapToInt(w -> w.getInProgressTasks() != null ? w.getInProgressTasks() : 0)
                .max()
                .orElse(0);

        int completedTasks = memberWorkloads.stream()
                .mapToInt(w -> w.getCompletedTasks() != null ? w.getCompletedTasks() : 0)
                .sum();

        String status = memberWorkloads.isEmpty() ? "BALANCED" :
                memberWorkloads.stream()
                        .map(w -> w.getStatus())
                        .reduce((a, b) -> a)
                        .orElse("BALANCED");

        int totalAssigned = taskRepository.findByAssigneeIdOrderByUpdatedAtDesc(member.getUser().getId()).size();

        return new TaskAssignmentPromptBuilder.MemberData(
                member.getId().toString(),
                member.getUser().getFullName(),
                member.getUser().getEmail(),
                member.getRole() != null ? member.getRole().getName() : "MEMBER",
                avgWorkload,
                openTasks,
                inProgressTasks,
                completedTasks,
                totalAssigned,
                status
        );
    }

    private String callGroq(String prompt) {
        if (!groqConfig.isValid()) {
            throw new AiException("Groq API key is not configured or invalid");
        }

        try {
            return aiService.generate(SYSTEM_PROMPT + "\n\n" + prompt);
        } catch (Exception e) {
            log.error("Groq API error: {}", e.getMessage());
            throw new AiException("Failed to get assignment recommendation: " + e.getMessage(), e);
        }
    }

    private TaskAssignmentResponse parseAndBuildResponse(
            String aiResponse,
            List<TaskAssignmentPromptBuilder.MemberData> memberDataList,
            long processingTime) {

        try {
            String json = extractJson(aiResponse);

            if (json == null || json.isBlank()) {
                return buildDefaultResponse(memberDataList, "Empty response from AI", processingTime);
            }

            JsonNode root = objectMapper.readTree(json);

            String recommendedAssignee = root.path("recommendedAssignee").asText(null);
            String recommendedAssigneeId = root.path("recommendedAssigneeId").asText(null);
            Double confidence = root.path("confidence").asDouble(0.5);
            String reason = root.path("reason").asText("Analysis completed");

            List<TaskAssignmentResponse.MemberRanking> ranking = parseRanking(root.path("ranking"), memberDataList);
            List<String> warnings = parseStringList(root.path("warnings"));

            return TaskAssignmentResponse.success(
                    recommendedAssignee,
                    recommendedAssigneeId,
                    confidence,
                    ranking,
                    warnings,
                    reason,
                    processingTime
            );

        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", e.getMessage());
            return buildDefaultResponse(memberDataList, "Failed to parse AI response", processingTime);
        }
    }

    private List<TaskAssignmentResponse.MemberRanking> parseRanking(
            JsonNode rankingNode,
            List<TaskAssignmentPromptBuilder.MemberData> memberDataList) {

        List<TaskAssignmentResponse.MemberRanking> ranking = new ArrayList<>();

        if (!rankingNode.isArray()) {
            return ranking;
        }

        for (JsonNode item : rankingNode) {
            String memberId = item.path("memberId").asText(null);

            TaskAssignmentResponse.MemberRanking rankingItem = TaskAssignmentResponse.MemberRanking.builder()
                    .memberId(memberId)
                    .memberName(item.path("memberName").asText("Unknown"))
                    .email(item.path("email").asText(""))
                    .score(item.path("score").asInt(50))
                    .currentWorkload(item.path("currentWorkload").asText("medium"))
                    .openTasks(item.path("openTasks").asInt(0))
                    .inProgressTasks(item.path("inProgressTasks").asInt(0))
                    .role(item.path("role").asText(""))
                    .reason(item.path("reason").asText(""))
                    .build();

            if (memberId != null) {
                memberDataList.stream()
                        .filter(m -> m.getId().equals(memberId))
                        .findFirst()
                        .ifPresent(m -> {
                            rankingItem.setOpenTasks(m.getOpenTasks());
                            rankingItem.setInProgressTasks(m.getInProgressTasks());
                        });
            }

            ranking.add(rankingItem);
        }

        return ranking;
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

    private String extractJson(String text) {
        if (text == null) return null;

        String trimmed = text.trim();
        int jsonStart = trimmed.indexOf('{');
        int jsonEnd = trimmed.lastIndexOf('}');

        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return trimmed.substring(jsonStart, jsonEnd + 1);
        }
        return trimmed;
    }

    private TaskAssignmentResponse buildDefaultResponse(
            List<TaskAssignmentPromptBuilder.MemberData> members,
            String errorMessage,
            long processingTime) {

        String bestMember = members.isEmpty() ? null : members.get(0).getName();
        String bestMemberId = members.isEmpty() ? null : members.get(0).getId();

        List<TaskAssignmentResponse.MemberRanking> ranking = members.stream()
                .limit(5)
                .map(m -> TaskAssignmentResponse.MemberRanking.builder()
                        .memberId(m.getId())
                        .memberName(m.getName())
                        .email(m.getEmail())
                        .score(50)
                        .currentWorkload(m.getWorkloadPercentage() > 80 ? "high" : m.getWorkloadPercentage() > 50 ? "medium" : "low")
                        .openTasks(m.getOpenTasks())
                        .inProgressTasks(m.getInProgressTasks())
                        .role(m.getRole())
                        .reason("Default ranking based on workload")
                        .build())
                .collect(Collectors.toList());

        return TaskAssignmentResponse.success(
                bestMember,
                bestMemberId,
                0.0,
                ranking,
                List.of("AI analysis unavailable - using default ranking"),
                "AI analysis failed: " + errorMessage,
                processingTime
        );
    }
}
