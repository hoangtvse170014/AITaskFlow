package com.taskflow.service.impl;

import com.taskflow.dto.response.DashboardResponse;
import com.taskflow.dto.response.TaskResponse;
import com.taskflow.entity.Task;
import com.taskflow.entity.TaskPriority;
import com.taskflow.entity.TaskStatus;
import com.taskflow.entity.User;
import com.taskflow.entity.Workspace;
import com.taskflow.exception.ForbiddenException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.repository.WorkspaceRepository;
import com.taskflow.service.DashboardService;
import com.taskflow.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final TaskRepository taskRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceService workspaceService;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboardStats(UUID workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", workspaceId));

        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(workspaceId, currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }

        int totalTasks = taskRepository.countByWorkspaceId(workspaceId);
        int completedTasks = taskRepository.countByWorkspaceIdAndStatus(workspaceId, TaskStatus.DONE);
        int inProgressTasks = taskRepository.countByWorkspaceIdAndStatus(workspaceId, TaskStatus.IN_PROGRESS);
        int todoTasks = taskRepository.countByWorkspaceIdAndStatus(workspaceId, TaskStatus.TODO);
        int delayedTasks = taskRepository.countOverdueByWorkspaceId(workspaceId, LocalDate.now(), TaskStatus.DONE);

        int completionRate = totalTasks > 0 ? (completedTasks * 100) / totalTasks : 0;

        Map<String, Integer> tasksByPriority = new HashMap<>();
        List<Task> allTasks = taskRepository.findAllByWorkspaceIdOrderByUpdatedAtDesc(workspaceId);

        for (TaskPriority priority : TaskPriority.values()) {
            int count = (int) allTasks.stream().filter(t -> t.getPriority() == priority).count();
            tasksByPriority.put(priority.name(), count);
        }

        Map<String, Integer> tasksByStatus = new HashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            int count = taskRepository.countByWorkspaceIdAndStatus(workspaceId, status);
            tasksByStatus.put(status.name(), count);
        }

        List<TaskResponse> recentTaskResponses = allTasks.stream()
                .limit(10)
                .map(TaskResponse::fromEntity)
                .collect(Collectors.toList());

        List<Task> overdueTasks = taskRepository.findOverdueTasksByWorkspaceId(workspaceId, LocalDate.now(), TaskStatus.DONE);
        List<TaskResponse> overdueTaskResponses = overdueTasks.stream()
                .limit(10)
                .map(TaskResponse::fromEntity)
                .collect(Collectors.toList());

        return DashboardResponse.builder()
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .inProgressTasks(inProgressTasks)
                .todoTasks(todoTasks)
                .delayedTasks(delayedTasks)
                .completionRate(completionRate)
                .tasksByPriority(tasksByPriority)
                .tasksByStatus(tasksByStatus)
                .recentTasks(recentTaskResponses)
                .overdueTasks(overdueTaskResponses)
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
