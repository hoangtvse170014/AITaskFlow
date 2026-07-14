package com.taskflow.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectPlannerResponse {

    private String projectName;
    private String description;
    private String scope;
    private List<Milestone> milestones;
    private List<Sprint> sprints;
    private List<Epic> epics;
    private List<Task> tasks;
    private List<Subtask> subtasks;
    private List<Dependency> dependencies;
    private Timeline timeline;
    private Double confidence;
    private Long processingTimeMs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Milestone {
        private String name;
        private String description;
        private String targetDate;
        private List<String> deliverables;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Sprint {
        private Integer number;
        private String name;
        private String goal;
        private String startDate;
        private String endDate;
        private List<String> tasks;
        private Integer capacity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Epic {
        private String id;
        private String name;
        private String description;
        private Priority priority;
        private Integer estimatedHours;
        private List<String> features;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Feature {
        private String id;
        private String name;
        private String description;
        private String epicId;
        private Priority priority;
        private Integer estimatedHours;
        private Integer storyPoints;
        private List<String> tasks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Task {
        private String id;
        private String title;
        private String description;
        private String epicId;
        private String featureId;
        private Priority priority;
        private Integer estimatedHours;
        private Integer storyPoints;
        private String suggestedAssignee;
        private List<String> acceptanceCriteria;
        private List<String> risks;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Subtask {
        private String id;
        private String parentTaskId;
        private String title;
        private Integer estimatedHours;
        private String suggestedAssignee;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Dependency {
        private String taskId;
        private String dependsOnTaskId;
        private String type;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Timeline {
        private String startDate;
        private String endDate;
        private Integer totalWeeks;
        private Integer totalEstimatedHours;
        private Integer totalStoryPoints;
    }

    public enum Priority {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }
}
