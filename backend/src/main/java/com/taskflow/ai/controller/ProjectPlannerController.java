package com.taskflow.ai.controller;

import com.taskflow.ai.dto.ProjectPlannerRequest;
import com.taskflow.ai.dto.ProjectPlannerResponse;
import com.taskflow.ai.service.ProjectPlannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/planner")
@RequiredArgsConstructor
@Slf4j
public class ProjectPlannerController {

    private final ProjectPlannerService projectPlannerService;

    @PostMapping
    public ResponseEntity<ProjectPlannerResponse> planProject(@Valid @RequestBody ProjectPlannerRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Received project planning request for idea: {}", 
                    request.getProjectIdea().substring(0, Math.min(50, request.getProjectIdea().length())));

            ProjectPlannerResponse response = projectPlannerService.planProject(request);

            log.info("Project planning completed in {}ms", System.currentTimeMillis() - startTime);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid project planning request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ProjectPlannerResponse.builder()
                            .projectName("Invalid Request")
                            .description("Validation error: " + e.getMessage())
                            .confidence(0.0)
                            .processingTimeMs(System.currentTimeMillis() - startTime)
                            .build());
        } catch (Exception e) {
            log.error("Project planning failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ProjectPlannerResponse.builder()
                            .projectName("Planning Failed")
                            .description("An error occurred: " + e.getMessage())
                            .confidence(0.0)
                            .processingTimeMs(System.currentTimeMillis() - startTime)
                            .build());
        }
    }
}
