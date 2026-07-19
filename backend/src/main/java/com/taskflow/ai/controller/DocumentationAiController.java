package com.taskflow.ai.controller;

import com.taskflow.ai.dto.DocumentType;
import com.taskflow.ai.dto.DocumentationRequest;
import com.taskflow.ai.dto.DocumentationResponse;
import com.taskflow.ai.service.DocumentationAiService;
import com.taskflow.ai.service.DocumentationPromptFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST endpoints for AI Documentation generation.
 *
 * <p>Two flavours are exposed:
 * <ul>
 *   <li>{@code POST /api/ai/documentation} with a body that includes
 *       {@code workspaceId} and {@code documentType}. This is the canonical,
 *       generic endpoint used by the UI when the user picks a type from a
 *       dropdown.</li>
 *   <li>Seven convenience endpoints, one per document type, for callers that
 *       prefer type-specific URLs (e.g. workflow automations).</li>
 * </ul>
 *
 * <p>All endpoints:
 * <ul>
 *   <li>Require authentication (enforced by {@code SecurityConfig}).</li>
 *   <li>Are read-only with respect to the database.</li>
 *   <li>Return a {@link DocumentationResponse} that always contains
 *       {@code documentType} and either {@code markdown} or {@code error}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/ai/documentation")
@RequiredArgsConstructor
@Slf4j
public class DocumentationAiController {

    private final DocumentationAiService documentationAiService;
    private final DocumentationPromptFactory promptFactory;

    /**
     * Generic endpoint — picks the type from the request body.
     */
    @PostMapping
    public ResponseEntity<DocumentationResponse> generate(@Valid @RequestBody DocumentationRequest request) {
        return runSafely(request);
    }

    /** Generate a Software Requirements Specification. */
    @PostMapping("/srs")
    public ResponseEntity<DocumentationResponse> generateSrs(@RequestBody DocumentationRequest request) {
        request.setDocumentType(DocumentType.SRS);
        return runSafely(request);
    }

    /** Generate User Stories. */
    @PostMapping("/user-stories")
    public ResponseEntity<DocumentationResponse> generateUserStories(@RequestBody DocumentationRequest request) {
        request.setDocumentType(DocumentType.USER_STORIES);
        return runSafely(request);
    }

    /** Generate Acceptance Criteria. */
    @PostMapping("/acceptance-criteria")
    public ResponseEntity<DocumentationResponse> generateAcceptanceCriteria(@RequestBody DocumentationRequest request) {
        request.setDocumentType(DocumentType.ACCEPTANCE_CRITERIA);
        return runSafely(request);
    }

    /** Generate Meeting Minutes. */
    @PostMapping("/meeting-minutes")
    public ResponseEntity<DocumentationResponse> generateMeetingMinutes(@RequestBody DocumentationRequest request) {
        request.setDocumentType(DocumentType.MEETING_MINUTES);
        return runSafely(request);
    }

    /** Generate a Sprint Review document. */
    @PostMapping("/sprint-review")
    public ResponseEntity<DocumentationResponse> generateSprintReview(@RequestBody DocumentationRequest request) {
        request.setDocumentType(DocumentType.SPRINT_REVIEW);
        return runSafely(request);
    }

    /** Generate a Sprint Retrospective. */
    @PostMapping("/retrospective")
    public ResponseEntity<DocumentationResponse> generateRetrospective(@RequestBody DocumentationRequest request) {
        request.setDocumentType(DocumentType.RETROSPECTIVE);
        return runSafely(request);
    }

    /** Generate a Technical Specification. */
    @PostMapping("/technical-spec")
    public ResponseEntity<DocumentationResponse> generateTechnicalSpec(@RequestBody DocumentationRequest request) {
        request.setDocumentType(DocumentType.TECHNICAL_SPEC);
        return runSafely(request);
    }

    /**
     * Return the list of supported document types. Useful for the frontend
     * to render the picker without hard-coding labels.
     */
    @GetMapping("/types")
    public ResponseEntity<List<Map<String, String>>> listTypes() {
        List<Map<String, String>> types = promptFactory.supportedTypes().stream()
                .map(t -> Map.of(
                        "id", t.name(),
                        "label", humanize(t),
                        "endpoint", endpointFor(t)))
                .toList();
        return ResponseEntity.ok(types);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private ResponseEntity<DocumentationResponse> runSafely(DocumentationRequest request) {
        long start = System.currentTimeMillis();
        try {
            if (request.getWorkspaceId() == null || request.getWorkspaceId().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(DocumentationResponse.error(request.getDocumentType(), "workspaceId is required"));
            }
            // Validate UUID early for a cleaner error message
            try {
                UUID.fromString(request.getWorkspaceId());
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest()
                        .body(DocumentationResponse.error(request.getDocumentType(),
                                "workspaceId is not a valid UUID"));
            }
            if (request.getDocumentType() == null) {
                return ResponseEntity.badRequest()
                        .body(DocumentationResponse.error(null, "documentType is required"));
            }

            log.info("Documentation generate: workspace={}, type={}, topic={}",
                    request.getWorkspaceId(), request.getDocumentType(), request.getTopic());
            DocumentationResponse response = documentationAiService.generate(request);
            log.info("Documentation generated in {}ms ({} chars of Markdown)",
                    System.currentTimeMillis() - start,
                    response.getMarkdown() == null ? 0 : response.getMarkdown().length());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Documentation request rejected: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(DocumentationResponse.error(request.getDocumentType(), e.getMessage()));
        } catch (Exception e) {
            log.error("Documentation generation failed for type {}: {}",
                    request.getDocumentType(), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(DocumentationResponse.error(request.getDocumentType(),
                            "Generation failed: " + e.getMessage()));
        }
    }

    private String humanize(DocumentType t) {
        return switch (t) {
            case SRS -> "Software Requirements Specification (SRS)";
            case USER_STORIES -> "User Stories";
            case ACCEPTANCE_CRITERIA -> "Acceptance Criteria";
            case MEETING_MINUTES -> "Meeting Minutes";
            case SPRINT_REVIEW -> "Sprint Review";
            case RETROSPECTIVE -> "Sprint Retrospective";
            case TECHNICAL_SPEC -> "Technical Specification";
        };
    }

    private String endpointFor(DocumentType t) {
        return switch (t) {
            case SRS -> "/api/ai/documentation/srs";
            case USER_STORIES -> "/api/ai/documentation/user-stories";
            case ACCEPTANCE_CRITERIA -> "/api/ai/documentation/acceptance-criteria";
            case MEETING_MINUTES -> "/api/ai/documentation/meeting-minutes";
            case SPRINT_REVIEW -> "/api/ai/documentation/sprint-review";
            case RETROSPECTIVE -> "/api/ai/documentation/retrospective";
            case TECHNICAL_SPEC -> "/api/ai/documentation/technical-spec";
        };
    }
}
