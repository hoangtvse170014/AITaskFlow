package com.taskflow.service.impl;

import com.taskflow.dto.request.SubTaskRequest;
import com.taskflow.dto.response.SubTaskResponse;
import com.taskflow.entity.SubTask;
import com.taskflow.entity.Task;
import com.taskflow.entity.User;
import com.taskflow.exception.ForbiddenException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.SubTaskRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.SubTaskService;
import com.taskflow.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubTaskServiceImpl implements SubTaskService {

    private final SubTaskRepository subTaskRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final WorkspaceService workspaceService;

    @Override
    public List<SubTaskResponse> getSubTasks(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        checkWorkspaceAccess(task);

        return subTaskRepository.findByTaskIdOrderByPositionAsc(taskId)
                .stream()
                .map(SubTaskResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SubTaskResponse createSubTask(UUID taskId, SubTaskRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        checkWorkspaceAccess(task);

        List<SubTask> existingSubTasks = subTaskRepository.findByTaskIdOrderByPositionAsc(taskId);
        int newPosition = existingSubTasks.isEmpty() ? 0 : existingSubTasks.get(existingSubTasks.size() - 1).getPosition() + 1;

        SubTask subTask = SubTask.builder()
                .task(task)
                .title(request.getTitle())
                .position(newPosition)
                .build();

        subTask = subTaskRepository.save(subTask);
        return SubTaskResponse.fromEntity(subTask);
    }

    @Override
    @Transactional
    public SubTaskResponse updateSubTask(UUID taskId, UUID subTaskId, SubTaskRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        checkWorkspaceAccess(task);

        SubTask subTask = subTaskRepository.findById(subTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("SubTask", "id", subTaskId));

        if (!subTask.getTask().getId().equals(taskId)) {
            throw new ResourceNotFoundException("SubTask", "id", subTaskId);
        }

        subTask.setTitle(request.getTitle());
        subTask = subTaskRepository.save(subTask);

        return SubTaskResponse.fromEntity(subTask);
    }

    @Override
    @Transactional
    public SubTaskResponse toggleSubTask(UUID taskId, UUID subTaskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        checkWorkspaceAccess(task);

        SubTask subTask = subTaskRepository.findById(subTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("SubTask", "id", subTaskId));

        if (!subTask.getTask().getId().equals(taskId)) {
            throw new ResourceNotFoundException("SubTask", "id", subTaskId);
        }

        subTask.setCompleted(!subTask.getCompleted());
        subTask = subTaskRepository.save(subTask);

        return SubTaskResponse.fromEntity(subTask);
    }

    @Override
    @Transactional
    public void deleteSubTask(UUID taskId, UUID subTaskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        checkWorkspaceAccess(task);

        SubTask subTask = subTaskRepository.findById(subTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("SubTask", "id", subTaskId));

        if (!subTask.getTask().getId().equals(taskId)) {
            throw new ResourceNotFoundException("SubTask", "id", subTaskId);
        }

        subTaskRepository.delete(subTask);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private void checkWorkspaceAccess(Task task) {
        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(task.getProject().getWorkspace().getId(), currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }
    }
}
