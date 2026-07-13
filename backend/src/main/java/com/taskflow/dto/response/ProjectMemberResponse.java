package com.taskflow.dto.response;

import com.taskflow.entity.ProjectMember;
import com.taskflow.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberResponse {

    private String id;
    private String projectId;
    private UserResponse user;
    private String role;
    private LocalDateTime createdAt;

    public static ProjectMemberResponse fromEntity(ProjectMember member) {
        String roleName = member.getRole() != null ? member.getRole().getName() : "MEMBER";
        return ProjectMemberResponse.builder()
                .id(member.getId().toString())
                .projectId(member.getProject().getId().toString())
                .user(UserResponse.fromEntity(member.getUser()))
                .role(roleName)
                .createdAt(member.getCreatedAt())
                .build();
    }
}
