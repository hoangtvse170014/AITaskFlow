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
public class TaskAssignmentResponse {

    private String recommendedAssignee;

    private String recommendedAssigneeId;

    private Double confidence;

    private List<MemberRanking> ranking;

    private List<String> warnings;

    private String reason;

    private Long processingTimeMs;

    private String error;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MemberRanking {
        private String memberId;
        private String memberName;
        private String email;
        private Integer score;
        private String currentWorkload;
        private Integer openTasks;
        private Integer inProgressTasks;
        private String role;
        private String reason;
    }

    public static TaskAssignmentResponse success(
            String recommendedAssignee,
            String recommendedAssigneeId,
            Double confidence,
            List<MemberRanking> ranking,
            List<String> warnings,
            String reason,
            Long processingTimeMs) {
        return TaskAssignmentResponse.builder()
                .recommendedAssignee(recommendedAssignee)
                .recommendedAssigneeId(recommendedAssigneeId)
                .confidence(confidence)
                .ranking(ranking)
                .warnings(warnings)
                .reason(reason)
                .processingTimeMs(processingTimeMs)
                .build();
    }

    public static TaskAssignmentResponse error(String error) {
        return TaskAssignmentResponse.builder()
                .error(error)
                .build();
    }
}
