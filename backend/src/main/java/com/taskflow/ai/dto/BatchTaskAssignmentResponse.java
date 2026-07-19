package com.taskflow.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchTaskAssignmentResponse {

    private String projectName;
    private List<TaskAssignment> assignments;
    private List<WorkloadSummary> workloadSummary;
    private Double overallConfidence;
    private List<String> warnings;
    private String reason;
    private Long processingTimeMs;
    private String error;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TaskAssignment {
        private String taskRef;
        private String title;
        private String assignedMemberId;
        private String assignedMemberName;
        private String roleMatched;
        private Double confidence;
        private String reason;
        private Boolean unassigned;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WorkloadSummary {
        private String memberId;
        private String memberName;
        private int assignedTaskCount;
        private double currentWorkloadPercent;
        private double estimatedNewWorkloadPercent;
    }

    public static BatchTaskAssignmentResponse error(String message) {
        return BatchTaskAssignmentResponse.builder()
                .error(message)
                .build();
    }
}