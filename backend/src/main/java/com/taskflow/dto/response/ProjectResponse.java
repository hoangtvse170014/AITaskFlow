package com.taskflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private String id;
    private String workspaceId;
    private String name;
    private String key;
    private String description;
    private String color;
    private String icon;
    private int taskCount;
    private int completedTaskCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
