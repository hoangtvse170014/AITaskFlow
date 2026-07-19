package com.taskflow.dto.response;

import com.taskflow.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String id;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String workspaceId;

    public static UserResponse fromEntity(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
    
    public static UserResponse fromEntityWithWorkspace(User user, String workspaceId) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .workspaceId(workspaceId)
                .build();
    }
}
