package com.taskflow.dto.response;

import com.taskflow.entity.TaskComment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCommentResponse {

    private String id;
    private String taskId;
    private UserResponse user;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TaskCommentResponse fromEntity(TaskComment comment) {
        return TaskCommentResponse.builder()
                .id(comment.getId().toString())
                .taskId(comment.getTask().getId().toString())
                .user(UserResponse.fromEntity(comment.getUser()))
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
