package com.taskflow.dto.response;

import com.taskflow.entity.WorkspaceMember;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceMemberResponse {

    private String id;
    private String workspaceId;
    private UserResponse user;
    private String role;
    private String roleName;
    private Integer rolePriority;
    private Set<String> permissions;
    private LocalDateTime invitedAt;
    private LocalDateTime joinedAt;
    private Boolean isActive;

    public static WorkspaceMemberResponse fromEntity(WorkspaceMember member) {
        WorkspaceMemberResponseBuilder builder = WorkspaceMemberResponse.builder()
                .id(member.getId().toString())
                .workspaceId(member.getWorkspace().getId().toString())
                .user(UserResponse.fromEntity(member.getUser()))
                .role(member.getRole() != null ? member.getRole().getName() : null)
                .invitedAt(member.getInvitedAt())
                .joinedAt(member.getJoinedAt())
                .isActive(member.getIsActive());

        if (member.getRole() != null) {
            builder.roleName(member.getRole().getName());
            builder.rolePriority(member.getRole().getPriority());
            builder.permissions(member.getRole().getPermissions().stream()
                    .map(p -> p.getName())
                    .collect(java.util.stream.Collectors.toSet()));
        }

        return builder.build();
    }
}
