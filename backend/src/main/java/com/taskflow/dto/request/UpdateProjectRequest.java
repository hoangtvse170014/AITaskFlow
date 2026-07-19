package com.taskflow.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProjectRequest {

    @Size(min = 2, max = 100, message = "Project name must be between 2 and 100 characters")
    private String name;

    @Pattern(regexp = "^[A-Z]{2,6}$", message = "Project key must be 2-6 uppercase letters")
    private String key;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color code")
    private String color;

    private String icon;
}
