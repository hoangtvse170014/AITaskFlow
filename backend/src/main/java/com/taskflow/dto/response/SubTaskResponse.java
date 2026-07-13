package com.taskflow.dto.response;

import com.taskflow.entity.SubTask;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubTaskResponse {

    private String id;
    private String taskId;
    private String title;
    private boolean completed;
    private int position;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SubTaskResponse fromEntity(SubTask subTask) {
        return SubTaskResponse.builder()
                .id(subTask.getId().toString())
                .taskId(subTask.getTask().getId().toString())
                .title(subTask.getTitle())
                .completed(subTask.getCompleted())
                .position(subTask.getPosition())
                .createdAt(subTask.getCreatedAt())
                .updatedAt(subTask.getUpdatedAt())
                .build();
    }
}
