package com.taskflow.ai.controller;

import com.taskflow.ai.dto.SprintGenerateRequest;
import com.taskflow.ai.dto.SprintGenerateResponse;
import com.taskflow.ai.dto.WorkspaceAnswerResponse;
import com.taskflow.ai.dto.WorkspaceQuestionRequest;
import com.taskflow.ai.service.SprintGenerationService;
import com.taskflow.ai.service.WorkspaceAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/workspace")
@RequiredArgsConstructor
@Slf4j
public class WorkspaceAssistantController {

    private final WorkspaceAssistantService workspaceAssistantService;
    private final SprintGenerationService sprintGenerationService;

    @PostMapping("/chat")
    public ResponseEntity<WorkspaceAnswerResponse> chat(@Valid @RequestBody WorkspaceQuestionRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            String questionPreview = request.getQuestion() == null
                    ? "null"
                    : request.getQuestion().substring(0, Math.min(80, request.getQuestion().length()));
            log.info("Received workspace chat request for workspace: {}, questionPreview: {}",
                    request.getWorkspaceId(), questionPreview);

            WorkspaceAnswerResponse response = workspaceAssistantService.answerQuestion(
                    request.getWorkspaceId(),
                    request.getQuestion()
            );

            log.info("Workspace chat completed in {}ms", System.currentTimeMillis() - startTime);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid workspace ID: {}", request.getWorkspaceId(), e);
            return ResponseEntity.badRequest()
                    .body(WorkspaceAnswerResponse.error("Workspace not found: " + request.getWorkspaceId()));
        } catch (IllegalStateException e) {
            log.warn("Invalid workspace state: {}", request.getWorkspaceId(), e);
            return ResponseEntity.badRequest()
                    .body(WorkspaceAnswerResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Workspace chat failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(WorkspaceAnswerResponse.error("Failed to process question: " + e.getMessage()));
        }
    }

    @PostMapping("/sprint/generate")
    public ResponseEntity<SprintGenerateResponse> generateSprint(@Valid @RequestBody SprintGenerateRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Received sprint generation request for workspace: {}", request.getWorkspaceId());

        try {
            SprintGenerateResponse response = sprintGenerationService.generateSprint(request);
            log.info("Sprint generation completed in {}ms", System.currentTimeMillis() - startTime);

            if (response.getError() != null) {
                return ResponseEntity.internalServerError().body(response);
            }
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid workspace ID for sprint: {}", request.getWorkspaceId(), e);
            return ResponseEntity.badRequest()
                    .body(SprintGenerateResponse.builder()
                            .error("Workspace not found: " + request.getWorkspaceId())
                            .confidence(0.0)
                            .processingTimeMs(System.currentTimeMillis() - startTime)
                            .build());
        } catch (Exception e) {
            log.error("Sprint generation failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(SprintGenerateResponse.builder()
                            .error("Sprint generation failed: " + e.getMessage())
                            .confidence(0.0)
                            .processingTimeMs(System.currentTimeMillis() - startTime)
                            .build());
        }
    }
}
