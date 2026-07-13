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
public class WorkspaceResponse {

    private String id;
    private String workspaceId;
    private String name;
    private String slug;
    private String description;
    private UserResponse owner;
    private String role;
    private int memberCount;
    private int projectCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
