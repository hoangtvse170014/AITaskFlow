package com.taskflow.dto.response;

import com.taskflow.entity.TaskActivityLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskActivityLogResponse {

    private String id;
    private String taskId;
    private UserResponse user;
    private String action;
    private String fieldChanged;
    private String oldValue;
    private String newValue;
    private LocalDateTime createdAt;

    public static TaskActivityLogResponse fromEntity(TaskActivityLog log) {
        return TaskActivityLogResponse.builder()
                .id(log.getId().toString())
                .taskId(log.getTask().getId().toString())
                .user(UserResponse.fromEntity(log.getUser()))
                .action(log.getAction())
                .fieldChanged(log.getFieldChanged())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
