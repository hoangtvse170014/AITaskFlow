package com.taskflow.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SprintGenerateResponse {

    private String sprintGoal;

    private List<SprintTask> tasks;

    private TeamCapacity capacity;

    private List<String> risks;

    private List<String> suggestions;

    private Double confidence;

    private Long processingTimeMs;

    private String error;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SprintTask {
        private String taskId;
        private String taskKey;
        private String title;
        private String projectName;
        private String priority;
        private String status;
        private String dueDate;
        private String suggestedAssigneeId;
        private String suggestedAssigneeName;
        private String effort; // S / M / L
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TeamCapacity {
        private int totalMembers;
        private int availableMembers;
        private int overloadedMembers;
        private List<MemberCapacity> breakdown;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MemberCapacity {
        private String memberId;
        private String name;
        private String role;
        private int currentWorkload;
        private String canTakeMore;
    }
}
