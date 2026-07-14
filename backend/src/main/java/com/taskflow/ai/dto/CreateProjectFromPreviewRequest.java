package com.taskflow.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Triggers autonomous creation of all entities from a PreviewProjectResponse.
 * The previewId is used for idempotency: if the same previewId is submitted again
 * and the project already exists, the existing project is returned without
 * duplicating any data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateProjectFromPreviewRequest {

    @NotNull(message = "workspaceId is required")
    private UUID workspaceId;

    /** Optional idempotency key. If omitted, the server uses the preview payload hash. */
    private String idempotencyKey;

    /** Full preview response from POST /api/ai/project/preview */
    @NotNull(message = "preview payload is required")
    private PreviewProjectResponse preview;
}