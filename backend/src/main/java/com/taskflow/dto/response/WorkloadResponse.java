package com.taskflow.dto.response;

import lombok.*;
import java.time.LocalDate;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkloadResponse {
    
    private UUID workspaceId;
    private List<MemberWorkloadData> members;
    private WorkloadHeatmap heatmap;
    private WorkloadSummary summary;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MemberWorkloadData {
        private UUID memberId;
        private String memberName;
        private String email;
        private String avatarUrl;
        private String role;
        private int openTasks;
        private int inProgressTasks;
        private int completedTasks;
        private int blockedTasks;
        private double hoursEstimated;
        private double hoursLogged;
        private int workloadPercentage;
        private String status;
        private List<DailyWorkload> weeklyWorkload;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyWorkload {
        private LocalDate date;
        private int openTasks;
        private int completedTasks;
        private int workloadPercentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WorkloadHeatmap {
        private List<String> dates;
        private List<HeatmapRow> rows;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HeatmapRow {
        private UUID memberId;
        private String memberName;
        private String avatarUrl;
        private List<Integer> values; // workload percentage for each date
        private List<String> statuses;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WorkloadSummary {
        private double averageWorkload;
        private int overloadedMembers;
        private int underutilizedMembers;
        private int balancedMembers;
        private double totalHoursLogged;
        private double totalHoursEstimated;
    }
}
