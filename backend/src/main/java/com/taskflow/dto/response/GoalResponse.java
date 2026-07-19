package com.taskflow.dto.response;

import lombok.*;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalResponse {
    
    private String id;
    private String workspaceId;
    private String ownerId;
    private String ownerName;
    private String title;
    private String description;
    private String type;
    private String status;
    private String period;
    private String startDate;
    private String dueDate;
    private Integer targetValue;
    private Integer currentValue;
    private Integer progressPercentage;
    private List<KeyResultResponse> keyResults;
    private Boolean isArchived;
    private String createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KeyResultResponse {
        private String id;
        private String title;
        private String metricType;
        private Double startValue;
        private Double targetValue;
        private Double currentValue;
        private Integer progressPercentage;
        private String dueDate;
        private String status;
        private String assigneeName;
    }
}
