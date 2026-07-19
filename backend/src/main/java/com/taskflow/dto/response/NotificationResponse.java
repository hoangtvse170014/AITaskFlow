package com.taskflow.dto.response;

import com.taskflow.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private String id;
    private String type;
    private String message;
    private boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationResponse fromEntity(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId().toString())
                .type(notification.getType())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
