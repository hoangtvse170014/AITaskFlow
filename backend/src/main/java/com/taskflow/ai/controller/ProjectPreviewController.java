package com.taskflow.ai.controller;

import com.taskflow.ai.dto.CreateProjectFromPreviewRequest;
import com.taskflow.ai.dto.CreateProjectFromPreviewResponse;
import com.taskflow.ai.dto.PreviewProjectRequest;
import com.taskflow.ai.dto.PreviewProjectResponse;
import com.taskflow.ai.service.AutonomousProjectCreationService;
import com.taskflow.ai.service.ProjectPreviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Read-only project preview. Generates a full project plan + assignment
 * preview via AI and returns it for the user to review BEFORE anything is
 * persisted to the database. No create endpoints are exposed here.
 *
 * The companion endpoint POST /create-from-preview materializes a preview
 * via the existing Project/Task/SubTask APIs with full rollback and
 * idempotency.
 */
@RestController
@RequestMapping("/api/ai/project")
@RequiredArgsConstructor
@Slf4j
public class ProjectPreviewController {

    private final ProjectPreviewService projectPreviewService;
    private final AutonomousProjectCreationService autonomousProjectCreationService;

    @PostMapping("/preview")
    public ResponseEntity<PreviewProjectResponse> preview(@Valid @RequestBody PreviewProjectRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Received project preview request for idea: {}",
                    request.getProjectIdea().substring(0, Math.min(50, request.getProjectIdea().length())));

            PreviewProjectResponse response = projectPreviewService.preview(request);

            log.info("Project preview completed in {}ms (previewId={})",
                    System.currentTimeMillis() - startTime, response.getPreviewId());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid preview request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(PreviewProjectResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Project preview failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(PreviewProjectResponse.error("Preview failed: " + e.getMessage()));
        }
    }

    /**
     * Autonomous creation. Drives Project API -> Task API -> SubTask API
     * (with sprint info encoded as task labels) -> Assignment via UpdateTask.
     * Any failure rolls back every created entity. Re-submitting the same
     * preview is a no-op (idempotent).
     */
    @PostMapping("/create-from-preview")
    public ResponseEntity<CreateProjectFromPreviewResponse> createFromPreview(
            @Valid @RequestBody CreateProjectFromPreviewRequest request) {

        long startTime = System.currentTimeMillis();

        try {
            log.info("Received autonomous create-from-preview: workspaceId={}, previewId={}",
                    request.getWorkspaceId(),
                    request.getPreview() != null ? request.getPreview().getPreviewId() : null);

            CreateProjectFromPreviewResponse response = autonomousProjectCreationService.createFromPreview(request);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Autonomous creation finished in {}ms: success={}, projectId={}",
                    elapsed, response.isSuccess(), response.getProjectId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Autonomous creation failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(CreateProjectFromPreviewResponse.fail("Autonomous creation failed: " + e.getMessage()));
        }
    }
}