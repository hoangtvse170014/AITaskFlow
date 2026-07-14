package com.taskflow.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectPlannerRequest {

    @NotBlank(message = "Project idea is required")
    @Size(min = 10, max = 2000, message = "Project idea must be between 10 and 2000 characters")
    private String projectIdea;

    private String teamSize;

    private String technologyStack;

    private Integer weeksDeadline;
}
