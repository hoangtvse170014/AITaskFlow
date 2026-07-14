package com.taskflow.ai.controller;

import com.taskflow.ai.dto.DemoModeRequest;
import com.taskflow.ai.dto.DemoModeResponse;
import com.taskflow.ai.service.DemoModeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Demo Mode endpoints. The user types a free-form project idea (e.g.
 * "Create a Grocery Management project") and a single call runs the
 * whole pipeline end-to-end.
 *
 * <p>This is the recommended entry point for new users because it removes
 * the two-step "preview then create" decision: they see the result in one
 * shot with progress stages the UI can render as an animated timeline.
 */
@RestController
@RequestMapping("/api/ai/demo")
@RequiredArgsConstructor
@Slf4j
public class DemoModeController {

    private final DemoModeService demoModeService;

    /**
     * One-shot demo run. The pipeline runs entirely inside the request:
     * analyzing -> planning -> generating -> assigning -> creating -> refreshing.
     */
    @PostMapping("/start")
    public ResponseEntity<DemoModeResponse> start(@Valid @RequestBody DemoModeRequest request) {
        long start = System.currentTimeMillis();
        log.info("Demo Mode start: workspace={}, idea='{}'", request.getWorkspaceId(),
                request.getProjectIdea().substring(0, Math.min(60, request.getProjectIdea().length())));
        try {
            DemoModeResponse response = demoModeService.run(request);
            log.info("Demo Mode finished in {}ms (success={}, project={})",
                    System.currentTimeMillis() - start, response.isSuccess(), response.getProjectId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Demo Mode rejected: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(DemoModeResponse.fail(e.getMessage(),
                            DemoModeResponse.DemoStage.ANALYZING_REQUIREMENTS));
        } catch (Exception e) {
            log.error("Demo Mode failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(DemoModeResponse.fail("Demo Mode failed: " + e.getMessage(),
                            DemoModeResponse.DemoStage.ANALYZING_REQUIREMENTS));
        }
    }

    /**
     * Cheap metadata endpoint describing what Demo Mode does. Useful for the
     * UI to show a tooltip / explainer card before the user clicks Start.
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
                "name", "Demo Mode",
                "description", "One-shot AI pipeline that plans, assigns, and creates a full project from a free-form idea.",
                "stages", List.of(
                        Map.of("id", "ANALYZING_REQUIREMENTS", "label", "Analyzing requirements", "icon", "🔍"),
                        Map.of("id", "PLANNING_SPRINTS", "label", "Planning sprints", "icon", "📅"),
                        Map.of("id", "GENERATING_TASKS", "label", "Generating tasks and subtasks", "icon", "🧩"),
                        Map.of("id", "ASSIGNING_MEMBERS", "label", "Assigning members", "icon", "👥"),
                        Map.of("id", "CREATING_ENTITIES", "label", "Creating project", "icon", "🛠️"),
                        Map.of("id", "REFRESHING_DASHBOARD", "label", "Refreshing dashboard", "icon", "🔄")),
                "defaultTargetTasks", "35-50",
                "defaultTargetSubtasks", "100+",
                "defaultSprints", 4,
                "idempotent", true
        ));
    }
}