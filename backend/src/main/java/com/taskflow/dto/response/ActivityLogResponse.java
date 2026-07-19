package com.taskflow.dto.response;

import com.taskflow.entity.ActivityLog;
import com.taskflow.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogResponse {

    private UUID id;
    private String action;
    private String entityType;
    private UUID entityId;
    private UserInfo user;
    private Map<String, Object> metadata;
    private String createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String id;
        private String email;
        private String fullName;
        private String avatarUrl;
    }

    public static ActivityLogResponse fromEntity(ActivityLog log) {
        if (log == null) return null;
        
        UserInfo userInfo = null;
        if (log.getUser() != null) {
            User user = log.getUser();
            userInfo = UserInfo.builder()
                    .id(user.getId().toString())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .avatarUrl(user.getAvatarUrl())
                    .build();
        }
        
        Map<String, Object> metadata = null;
        if (log.getMetadata() != null && !log.getMetadata().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                metadata = mapper.readValue(log.getMetadata(), Map.class);
            } catch (Exception e) {
                metadata = Map.of("raw", log.getMetadata());
            }
        }
        
        return ActivityLogResponse.builder()
                .id(log.getId())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .user(userInfo)
                .metadata(metadata)
                .createdAt(log.getCreatedAt() != null ? log.getCreatedAt().toString() : null)
                .build();
    }
}
