package com.taskflow.dto.response;

import com.taskflow.entity.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private String id;
    private String projectId;
    private String taskKey;
    private Integer taskNumber;
    private String title;
    private String description;
    private String status;
    private String priority;
    private UserResponse assignee;
    private UserResponse reporter;
    private LocalDate dueDate;
    private boolean overdue;
    private List<LabelResponse> labels;
    private List<ChecklistItemResponse> checklist;
    private int completedChecklistItems;
    private int totalChecklistItems;
    private Integer position;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabelResponse {
        private String name;
        private String color;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChecklistItemResponse {
        private String id;
        private String text;
        private boolean completed;
    }

    public static TaskResponse fromEntity(Task task) {
        int completedCount = 0;
        int totalCount = 0;
        if (task.getChecklist() != null) {
            totalCount = task.getChecklist().size();
            completedCount = (int) task.getChecklist().stream().filter(Task.ChecklistItem::isCompleted).count();
        }

        return TaskResponse.builder()
                .id(task.getId().toString())
                .projectId(task.getProject().getId().toString())
                .taskKey(task.getTaskKey())
                .taskNumber(task.getTaskNumber())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus().name())
                .priority(task.getPriority().name())
                .assignee(task.getAssignee() != null ? UserResponse.fromEntity(task.getAssignee()) : null)
                .reporter(UserResponse.fromEntity(task.getReporter()))
                .dueDate(task.getDueDate())
                .overdue(task.isOverdue())
                .labels(task.getLabels() != null ? task.getLabels().stream()
                        .map(l -> LabelResponse.builder().name(l.getName()).color(l.getColor()).build())
                        .toList() : null)
                .checklist(task.getChecklist() != null ? task.getChecklist().stream()
                        .map(c -> ChecklistItemResponse.builder()
                                .id(c.getId())
                                .text(c.getText())
                                .completed(c.isCompleted())
                                .build())
                        .toList() : null)
                .completedChecklistItems(completedCount)
                .totalChecklistItems(totalCount)
                .position(task.getPosition())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
