package com.taskflow.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceQuestionRequest {

    @NotNull(message = "Workspace ID is required")
    private UUID workspaceId;

    @NotBlank(message = "Question is required")
    private String question;
}
