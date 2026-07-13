package com.taskflow.ai.controller;

import com.taskflow.ai.config.GeminiConfig;
import com.taskflow.ai.dto.AiRequest;
import com.taskflow.ai.dto.AiResponse;
import com.taskflow.ai.dto.ProjectAnalysisResponse;
import com.taskflow.ai.dto.TaskAssignmentRequest;
import com.taskflow.ai.dto.TaskAssignmentResponse;
import com.taskflow.ai.service.AiProjectAnalyzerService;
import com.taskflow.ai.service.AiService;
import com.taskflow.ai.service.AiTaskAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class GeminiAiController {

    private final AiService aiService;
    private final AiProjectAnalyzerService projectAnalyzerService;
    private final AiTaskAssignmentService taskAssignmentService;
    private final GeminiConfig geminiConfig;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = Map.of(
                "enabled", geminiConfig.isValid(),
                "provider", "Google Gemini",
                "model", geminiConfig.getModel(),
                "status", geminiConfig.isValid() ? "READY" : "CONFIGURE_API_KEY"
        );
        return ResponseEntity.ok(health);
    }

    @PostMapping("/test")
    public ResponseEntity<AiResponse> testEndpoint(@Valid @RequestBody AiRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            String response = aiService.generate(request.getPrompt());
            long processingTime = System.currentTimeMillis() - startTime;

            return ResponseEntity.ok(AiResponse.success(response, geminiConfig.getModel(), processingTime));
        } catch (Exception e) {
            log.error("AI test request failed: {}", e.getMessage());
            long processingTime = System.currentTimeMillis() - startTime;
            return ResponseEntity.internalServerError()
                    .body(AiResponse.error(e.getMessage(), geminiConfig.getModel()));
        }
    }

    @PostMapping("/projects/{projectId}/analyze")
    public ResponseEntity<ProjectAnalysisResponse> analyzeProject(@PathVariable UUID projectId) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Received project analysis request for project: {}", projectId);

            ProjectAnalysisResponse response = projectAnalyzerService.analyzeProject(projectId);

            log.info("Project analysis completed in {}ms", System.currentTimeMillis() - startTime);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid project ID: {}", projectId, e);
            return ResponseEntity.badRequest()
                    .body(ProjectAnalysisResponse.error("Project not found: " + projectId));
        } catch (IllegalStateException e) {
            log.warn("Invalid project state: {}", projectId, e);
            return ResponseEntity.badRequest()
                    .body(ProjectAnalysisResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Project analysis failed for {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ProjectAnalysisResponse.error("Analysis failed: " + e.getMessage()));
        }
    }

    @PostMapping("/tasks/recommend")
    public ResponseEntity<TaskAssignmentResponse> recommendAssignee(@Valid @RequestBody TaskAssignmentRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Received task assignment recommendation request for: {}", request.getTitle());

            TaskAssignmentResponse response = taskAssignmentService.recommend(request);

            log.info("Task assignment recommendation completed in {}ms", System.currentTimeMillis() - startTime);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(TaskAssignmentResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Task assignment recommendation failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(TaskAssignmentResponse.error("Recommendation failed: " + e.getMessage()));
        }
    }
}
