package com.taskflow.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskRequest {

    private String title;
    private String description;
    private String status;
    private String priority;
    private String assigneeId;
    private LocalDate dueDate;
    private List<LabelDto> labels;
    private List<ChecklistItemDto> checklist;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabelDto {
        private String id;
        private String name;
        private String color;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChecklistItemDto {
        private String id;
        private String text;
        private boolean completed;
    }
}
