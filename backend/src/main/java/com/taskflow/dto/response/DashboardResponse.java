package com.taskflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private int totalTasks;
    private int completedTasks;
    private int inProgressTasks;
    private int todoTasks;
    private int delayedTasks;
    private int completionRate;
    private Map<String, Integer> tasksByPriority;
    private Map<String, Integer> tasksByStatus;
    private List<TaskResponse> recentTasks;
    private List<TaskResponse> overdueTasks;
}
