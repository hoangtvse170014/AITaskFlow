package com.taskflow.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.ai.dto.ProjectPlannerRequest;
import com.taskflow.ai.dto.ProjectPlannerResponse;
import com.taskflow.ai.dto.ProjectPlannerResponse.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectPlannerService {

    private final AiService aiService;
    private final ObjectMapper objectMapper;

    public ProjectPlannerResponse planProject(ProjectPlannerRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(request);
            String aiResponse = aiService.generateWithSystemPrompt(systemPrompt, userPrompt);

            long processingTime = System.currentTimeMillis() - startTime;
            log.info("Project planner AI response received in {}ms, response length: {}", processingTime, aiResponse.length());

            return parseResponse(aiResponse, processingTime);
        } catch (Exception e) {
            log.error("Project planner failed: {}", e.getMessage(), e);
            return buildErrorResponse("Failed to generate project plan: " + e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }

    private String buildSystemPrompt() {
        return """
            You are a Senior Agile Project Manager and Scrum Master with 15+ years of experience.
            Your task is to create a comprehensive project plan based on a project idea.

            IMPORTANT RULES:
            1. Return ONLY valid JSON, no markdown, no explanation, no text outside the JSON
            2. The JSON must be parseable by a standard JSON parser
            3. All fields must be properly quoted strings or numbers
            4. Arrays must be properly formatted
            5. Generate realistic estimates based on the project complexity

            OUTPUT SCHEMA:
            {
              "projectName": "string",
              "description": "string",
              "scope": "string",
              "timeline": {
                "startDate": "YYYY-MM-DD",
                "endDate": "YYYY-MM-DD",
                "totalWeeks": number,
                "totalEstimatedHours": number,
                "totalStoryPoints": number
              },
              "milestones": [
                {
                  "name": "string",
                  "description": "string",
                  "targetDate": "YYYY-MM-DD",
                  "deliverables": ["string"]
                }
              ],
              "sprints": [
                {
                  "number": number,
                  "name": "string",
                  "goal": "string",
                  "startDate": "YYYY-MM-DD",
                  "endDate": "YYYY-MM-DD",
                  "tasks": ["string"],
                  "capacity": number
                }
              ],
              "epics": [
                {
                  "id": "EPIC-001",
                  "name": "string",
                  "description": "string",
                  "priority": "HIGH|MEDIUM|LOW",
                  "estimatedHours": number,
                  "features": ["string"]
                }
              ],
              "tasks": [
                {
                  "id": "TASK-001",
                  "title": "string",
                  "description": "string",
                  "epicId": "EPIC-001",
                  "featureId": "FEAT-001",
                  "priority": "CRITICAL|HIGH|MEDIUM|LOW",
                  "estimatedHours": number,
                  "storyPoints": number,
                  "suggestedAssignee": "Backend Dev|Frontend Dev|Full Stack|Designer|QA",
                  "acceptanceCriteria": ["string"],
                  "risks": ["string"],
                  "status": "TODO"
                }
              ],
              "subtasks": [
                {
                  "id": "SUB-001",
                  "parentTaskId": "TASK-001",
                  "title": "string",
                  "estimatedHours": number,
                  "suggestedAssignee": "string"
                }
              ],
              "dependencies": [
                {
                  "taskId": "TASK-002",
                  "dependsOnTaskId": "TASK-001",
                  "type": "finish_to_start",
                  "reason": "string"
                }
              ],
              "confidence": 0.0-1.0,
              "processingTimeMs": number
            }
            """;
    }

    private String buildUserPrompt(ProjectPlannerRequest request) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Create a comprehensive project plan for the following idea:\n\n");
        prompt.append("PROJECT IDEA: ").append(request.getProjectIdea()).append("\n\n");

        if (request.getTeamSize() != null && !request.getTeamSize().isBlank()) {
            prompt.append("TEAM SIZE: ").append(request.getTeamSize()).append("\n");
        }

        if (request.getTechnologyStack() != null && !request.getTechnologyStack().isBlank()) {
            prompt.append("TECHNOLOGY STACK: ").append(request.getTechnologyStack()).append("\n");
        }

        if (request.getWeeksDeadline() != null) {
            prompt.append("DEADLINE: ").append(request.getWeeksDeadline()).append(" weeks\n");
        } else {
            prompt.append("DEADLINE: 12 weeks (default)\n");
        }

        prompt.append("\nGenerate:\n");
        prompt.append("- Project name and description\n");
        prompt.append("- Scope (in-scope and out-of-scope)\n");
        prompt.append("- 4-6 milestones with dates\n");
        prompt.append("- 6-8 sprints (2 weeks each)\n");
        prompt.append("- 4-6 epics\n");
        prompt.append("- 20-30 tasks with estimates\n");
        prompt.append("- 30-50 subtasks\n");
        prompt.append("- Dependencies between tasks\n");
        prompt.append("- Priority for all items\n");
        prompt.append("- Estimated hours and story points\n");
        prompt.append("- Suggested assignees\n");
        prompt.append("- Acceptance criteria\n");
        prompt.append("- Risks\n");
        prompt.append("- Timeline\n");

        return prompt.toString();
    }

    private ProjectPlannerResponse parseResponse(String aiResponse, long processingTime) {
        try {
            String json = extractJson(aiResponse);

            if (json == null || json.isBlank()) {
                return buildErrorResponse("Empty response from AI", processingTime);
            }

            JsonNode root = objectMapper.readTree(json);

            ProjectPlannerResponse.ProjectPlannerResponseBuilder builder = ProjectPlannerResponse.builder();

            builder.projectName(getTextValue(root, "projectName"));
            builder.description(getTextValue(root, "description"));
            builder.scope(getTextValue(root, "scope"));
            builder.confidence(root.path("confidence").asDouble(0.7));
            builder.processingTimeMs(processingTime);

            // Parse timeline
            JsonNode timelineNode = root.path("timeline");
            if (!timelineNode.isMissingNode()) {
                builder.timeline(Timeline.builder()
                        .startDate(getTextValue(timelineNode, "startDate"))
                        .endDate(getTextValue(timelineNode, "endDate"))
                        .totalWeeks(timelineNode.path("totalWeeks").asInt(12))
                        .totalEstimatedHours(timelineNode.path("totalEstimatedHours").asInt(0))
                        .totalStoryPoints(timelineNode.path("totalStoryPoints").asInt(0))
                        .build());
            }

            // Parse milestones
            builder.milestones(parseMilestones(root.path("milestones")));

            // Parse sprints
            builder.sprints(parseSprints(root.path("sprints")));

            // Parse epics
            builder.epics(parseEpics(root.path("epics")));

            // Parse tasks
            builder.tasks(parseTasks(root.path("tasks")));

            // Parse subtasks
            builder.subtasks(parseSubtasks(root.path("subtasks")));

            // Parse dependencies
            builder.dependencies(parseDependencies(root.path("dependencies")));

            return builder.build();

        } catch (Exception e) {
            log.error("Failed to parse project planner response: {}", e.getMessage());
            return buildErrorResponse("Failed to parse AI response: " + e.getMessage(), processingTime);
        }
    }

    private List<Milestone> parseMilestones(JsonNode node) {
        List<Milestone> milestones = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                List<String> deliverables = new ArrayList<>();
                JsonNode delNode = item.path("deliverables");
                if (delNode.isArray()) {
                    for (JsonNode d : delNode) {
                        deliverables.add(d.asText());
                    }
                }

                milestones.add(Milestone.builder()
                        .name(getTextValue(item, "name"))
                        .description(getTextValue(item, "description"))
                        .targetDate(getTextValue(item, "targetDate"))
                        .deliverables(deliverables)
                        .build());
            }
        }
        return milestones;
    }

    private List<Sprint> parseSprints(JsonNode node) {
        List<Sprint> sprints = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                List<String> tasks = new ArrayList<>();
                JsonNode tasksNode = item.path("tasks");
                if (tasksNode.isArray()) {
                    for (JsonNode t : tasksNode) {
                        tasks.add(t.asText());
                    }
                }

                sprints.add(Sprint.builder()
                        .number(item.path("number").asInt(1))
                        .name(getTextValue(item, "name"))
                        .goal(getTextValue(item, "goal"))
                        .startDate(getTextValue(item, "startDate"))
                        .endDate(getTextValue(item, "endDate"))
                        .tasks(tasks)
                        .capacity(item.path("capacity").asInt(40))
                        .build());
            }
        }
        return sprints;
    }

    private List<Epic> parseEpics(JsonNode node) {
        List<Epic> epics = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                List<String> features = new ArrayList<>();
                JsonNode featNode = item.path("features");
                if (featNode.isArray()) {
                    for (JsonNode f : featNode) {
                        features.add(f.asText());
                    }
                }

                String priorityStr = getTextValue(item, "priority");
                Priority priority = parsePriority(priorityStr);

                epics.add(Epic.builder()
                        .id(getTextValue(item, "id"))
                        .name(getTextValue(item, "name"))
                        .description(getTextValue(item, "description"))
                        .priority(priority)
                        .estimatedHours(item.path("estimatedHours").asInt(0))
                        .features(features)
                        .build());
            }
        }
        return epics;
    }

    private List<Task> parseTasks(JsonNode node) {
        List<Task> tasks = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                List<String> criteria = new ArrayList<>();
                JsonNode criteriaNode = item.path("acceptanceCriteria");
                if (criteriaNode.isArray()) {
                    for (JsonNode c : criteriaNode) {
                        criteria.add(c.asText());
                    }
                }

                List<String> risks = new ArrayList<>();
                JsonNode risksNode = item.path("risks");
                if (risksNode.isArray()) {
                    for (JsonNode r : risksNode) {
                        risks.add(r.asText());
                    }
                }

                String priorityStr = getTextValue(item, "priority");
                Priority priority = parsePriority(priorityStr);

                tasks.add(Task.builder()
                        .id(getTextValue(item, "id"))
                        .title(getTextValue(item, "title"))
                        .description(getTextValue(item, "description"))
                        .epicId(getTextValue(item, "epicId"))
                        .featureId(getTextValue(item, "featureId"))
                        .priority(priority)
                        .estimatedHours(item.path("estimatedHours").asInt(0))
                        .storyPoints(item.path("storyPoints").asInt(0))
                        .suggestedAssignee(getTextValue(item, "suggestedAssignee"))
                        .acceptanceCriteria(criteria)
                        .risks(risks)
                        .status(getTextValue(item, "status", "TODO"))
                        .build());
            }
        }
        return tasks;
    }

    private List<Subtask> parseSubtasks(JsonNode node) {
        List<Subtask> subtasks = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                subtasks.add(Subtask.builder()
                        .id(getTextValue(item, "id"))
                        .parentTaskId(getTextValue(item, "parentTaskId"))
                        .title(getTextValue(item, "title"))
                        .estimatedHours(item.path("estimatedHours").asInt(0))
                        .suggestedAssignee(getTextValue(item, "suggestedAssignee"))
                        .build());
            }
        }
        return subtasks;
    }

    private List<Dependency> parseDependencies(JsonNode node) {
        List<Dependency> deps = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                deps.add(Dependency.builder()
                        .taskId(getTextValue(item, "taskId"))
                        .dependsOnTaskId(getTextValue(item, "dependsOnTaskId"))
                        .type(getTextValue(item, "type"))
                        .reason(getTextValue(item, "reason"))
                        .build());
            }
        }
        return deps;
    }

    private Priority parsePriority(String value) {
        if (value == null) return Priority.MEDIUM;
        return switch (value.toUpperCase()) {
            case "CRITICAL" -> Priority.CRITICAL;
            case "HIGH" -> Priority.HIGH;
            case "LOW" -> Priority.LOW;
            default -> Priority.MEDIUM;
        };
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

    private String getTextValue(JsonNode node, String field) {
        return getTextValue(node, field, "");
    }

    private String getTextValue(JsonNode node, String field, String defaultValue) {
        JsonNode fieldNode = node.path(field);
        return fieldNode.isMissingNode() ? defaultValue : fieldNode.asText(defaultValue);
    }

    private ProjectPlannerResponse buildErrorResponse(String errorMessage, long processingTime) {
        return ProjectPlannerResponse.builder()
                .projectName("Project Plan (Error)")
                .description("Failed to generate project plan: " + errorMessage)
                .confidence(0.0)
                .processingTimeMs(processingTime)
                .build();
    }
}
