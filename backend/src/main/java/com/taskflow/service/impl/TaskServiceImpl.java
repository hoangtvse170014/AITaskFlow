package com.taskflow.service.impl;

import com.taskflow.dto.request.CreateCommentRequest;
import com.taskflow.dto.request.CreateTaskRequest;
import com.taskflow.dto.request.UpdateTaskPositionRequest;
import com.taskflow.dto.request.UpdateTaskRequest;
import com.taskflow.dto.response.TaskActivityLogResponse;
import com.taskflow.dto.response.TaskCommentResponse;
import com.taskflow.dto.response.TaskResponse;
import com.taskflow.entity.*;
import com.taskflow.exception.ForbiddenException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.*;
import com.taskflow.service.NotificationService;
import com.taskflow.service.TaskService;
import com.taskflow.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final TaskActivityLogRepository taskActivityLogRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final SubTaskRepository subTaskRepository;
    private final WorkspaceService workspaceService;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<TaskResponse>> getTasksByStatus(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(project.getWorkspace().getId(), currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }

        List<Task> tasks = taskRepository.findAllByProjectIdOrderByPosition(projectId);

        Map<String, List<TaskResponse>> grouped = new LinkedHashMap<>();
        grouped.put("TODO", new ArrayList<>());
        grouped.put("IN_PROGRESS", new ArrayList<>());
        grouped.put("REVIEW", new ArrayList<>());
        grouped.put("DONE", new ArrayList<>());

        for (Task task : tasks) {
            String status = task.getStatus().name();
            grouped.get(status).add(TaskResponse.fromEntity(task));
        }

        return grouped;
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(UUID projectId, UUID taskId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(project.getWorkspace().getId(), currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }

        Task task = taskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        return TaskResponse.fromEntity(task);
    }

    @Override
    @Transactional
    public TaskResponse createTask(UUID projectId, CreateTaskRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(project.getWorkspace().getId(), currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }

        Integer maxTaskNumber = taskRepository.findMaxTaskNumberByProjectId(projectId);
        int newTaskNumber = (maxTaskNumber != null ? maxTaskNumber : 0) + 1;

        List<Task.Label> labels = new ArrayList<>();
        if (request.getLabels() != null) {
            for (CreateTaskRequest.LabelDto dto : request.getLabels()) {
                Task.Label label = Task.Label.builder()
                        .id(dto.getId() != null ? dto.getId() : UUID.randomUUID().toString())
                        .name(dto.getName())
                        .color(dto.getColor())
                        .build();
                labels.add(label);
            }
        }

        List<Task.ChecklistItem> checklist = new ArrayList<>();
        if (request.getChecklist() != null) {
            for (CreateTaskRequest.ChecklistItemDto dto : request.getChecklist()) {
                Task.ChecklistItem item = Task.ChecklistItem.builder()
                        .id(dto.getId() != null ? dto.getId() : UUID.randomUUID().toString())
                        .text(dto.getText())
                        .completed(dto.isCompleted())
                        .build();
                checklist.add(item);
            }
        }

        Task task = Task.builder()
                .project(project)
                .taskNumber(newTaskNumber)
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TaskStatus.valueOf(request.getStatus()))
                .priority(TaskPriority.valueOf(request.getPriority()))
                .reporter(currentUser)
                .dueDate(request.getDueDate())
                .labels(labels)
                .checklist(checklist)
                .position(taskRepository.findAllByProjectIdAndStatusOrderByPosition(projectId,
                        TaskStatus.valueOf(request.getStatus())).size())
                .build();

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(UUID.fromString(request.getAssigneeId()))
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssigneeId()));
            task.setAssignee(assignee);
            notificationService.createNotification(
                assignee.getId(),
                "TASK_ASSIGNED",
                "You have been assigned to task: " + request.getTitle()
            );
        }

        task = taskRepository.save(task);
        createTaskActivityLog(task, currentUser, "CREATED", null, null, null);

        return TaskResponse.fromEntity(task);
    }

    @Override
    @Transactional
    public TaskResponse updateTask(UUID projectId, UUID taskId, UpdateTaskRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(project.getWorkspace().getId(), currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }

        Task task = taskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        if (request.getTitle() != null && !request.getTitle().equals(task.getTitle())) {
            createTaskActivityLog(task, currentUser, "UPDATED", "title", task.getTitle(), request.getTitle());
            task.setTitle(request.getTitle());
        }

        if (request.getDescription() != null && !request.getDescription().equals(task.getDescription())) {
            createTaskActivityLog(task, currentUser, "UPDATED", "description", task.getDescription(), request.getDescription());
            task.setDescription(request.getDescription());
        }

        if (request.getStatus() != null) {
            TaskStatus newStatus = TaskStatus.valueOf(request.getStatus());
            if (newStatus != task.getStatus()) {
                createTaskActivityLog(task, currentUser, "UPDATED", "status", task.getStatus().name(), newStatus.name());
                task.setStatus(newStatus);
            }
        }

        if (request.getPriority() != null) {
            TaskPriority newPriority = TaskPriority.valueOf(request.getPriority());
            if (newPriority != task.getPriority()) {
                createTaskActivityLog(task, currentUser, "UPDATED", "priority", task.getPriority().name(), newPriority.name());
                task.setPriority(newPriority);
            }
        }

        if (request.getAssigneeId() != null) {
            User newAssignee = userRepository.findById(UUID.fromString(request.getAssigneeId()))
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssigneeId()));
            String oldAssignee = task.getAssignee() != null ? task.getAssignee().getFullName() : "None";
            createTaskActivityLog(task, currentUser, "UPDATED", "assignee", oldAssignee, newAssignee.getFullName());
            task.setAssignee(newAssignee);
            notificationService.createNotification(
                newAssignee.getId(),
                "TASK_ASSIGNED",
                "You have been assigned to task: " + task.getTitle()
            );
        }

        if (request.getDueDate() != null && !request.getDueDate().equals(task.getDueDate())) {
            String oldDate = task.getDueDate() != null ? task.getDueDate().toString() : "None";
            createTaskActivityLog(task, currentUser, "UPDATED", "dueDate", oldDate, request.getDueDate().toString());
            task.setDueDate(request.getDueDate());
        }

        if (request.getLabels() != null) {
            List<Task.Label> labels = new ArrayList<>();
            for (UpdateTaskRequest.LabelDto dto : request.getLabels()) {
                Task.Label label = Task.Label.builder()
                        .id(dto.getId() != null ? dto.getId() : UUID.randomUUID().toString())
                        .name(dto.getName())
                        .color(dto.getColor())
                        .build();
                labels.add(label);
            }
            task.setLabels(labels);
        }

        if (request.getChecklist() != null) {
            List<Task.ChecklistItem> checklist = new ArrayList<>();
            for (UpdateTaskRequest.ChecklistItemDto dto : request.getChecklist()) {
                Task.ChecklistItem item = Task.ChecklistItem.builder()
                        .id(dto.getId() != null ? dto.getId() : UUID.randomUUID().toString())
                        .text(dto.getText())
                        .completed(dto.isCompleted())
                        .build();
                checklist.add(item);
            }
            task.setChecklist(checklist);
        }

        task = taskRepository.save(task);
        return TaskResponse.fromEntity(task);
    }

    @Override
    @Transactional
    public TaskResponse updateTaskPosition(UUID projectId, UUID taskId, UpdateTaskPositionRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(project.getWorkspace().getId(), currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }

        Task task = taskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        TaskStatus newStatus = TaskStatus.valueOf(request.getStatus());
        if (newStatus != task.getStatus()) {
            createTaskActivityLog(task, currentUser, "UPDATED", "status", task.getStatus().name(), newStatus.name());
            task.setStatus(newStatus);
        }

        if (request.getPosition() != null) {
            task.setPosition(request.getPosition());
        }

        task = taskRepository.save(task);
        return TaskResponse.fromEntity(task);
    }

    @Override
    @Transactional
    public void deleteTask(UUID projectId, UUID taskId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(project.getWorkspace().getId(), currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }

        Task task = taskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        // Cascade cleanup so the FK constraint subtasks.task_id -> tasks.id
        // doesn't reject the delete.
        subTaskRepository.deleteByTaskId(taskId);
        subTaskRepository.flush();

        taskRepository.delete(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskCommentResponse> getTaskComments(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(task.getProject().getWorkspace().getId(), currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }

        return taskCommentRepository.findAllByTaskIdOrderByCreatedAtAsc(taskId)
                .stream()
                .map(TaskCommentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TaskCommentResponse addComment(UUID taskId, CreateCommentRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(task.getProject().getWorkspace().getId(), currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }

        TaskComment taskComment = TaskComment.builder()
                .task(task)
                .user(currentUser)
                .content(request.getContent())
                .build();

        taskComment = taskCommentRepository.save(taskComment);

        if (task.getAssignee() != null && !task.getAssignee().getId().equals(currentUser.getId())) {
            notificationService.createNotification(
                task.getAssignee().getId(),
                "NEW_COMMENT",
                currentUser.getFullName() + " commented on: " + task.getTitle()
            );
        }

        if (!task.getReporter().getId().equals(currentUser.getId())
            && (task.getAssignee() == null || !task.getAssignee().getId().equals(currentUser.getId()))) {
            notificationService.createNotification(
                task.getReporter().getId(),
                "NEW_COMMENT",
                currentUser.getFullName() + " commented on: " + task.getTitle()
            );
        }

        return TaskCommentResponse.fromEntity(taskComment);
    }

    @Override
    @Transactional
    public void deleteComment(UUID taskId, UUID commentId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(task.getProject().getWorkspace().getId(), currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }

        TaskComment taskComment = taskCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        if (!taskComment.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only delete your own comments");
        }

        taskCommentRepository.delete(taskComment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskActivityLogResponse> getTaskActivityLogs(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(task.getProject().getWorkspace().getId(), currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }

        return taskActivityLogRepository.findAllByTaskIdOrderByCreatedAtDesc(taskId)
                .stream()
                .map(TaskActivityLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private void createTaskActivityLog(Task task, User user, String action, String fieldChanged,
                                   String oldValue, String newValue) {
        TaskActivityLog taskActivityLog = TaskActivityLog.builder()
                .task(task)
                .user(user)
                .action(action)
                .fieldChanged(fieldChanged)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();

        taskActivityLogRepository.save(taskActivityLog);
    }
    
    @Override
    public Project resolveProjectIdentifier(String projectIdentifier) {
        User currentUser = getCurrentUser();
        
        // Try to find by UUID first
        try {
            UUID projectId = UUID.fromString(projectIdentifier);
            return projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        } catch (IllegalArgumentException e) {
            // Not a UUID, try to find by key
        }
        
        // Find project by key - need to get workspace from user memberships
        List<WorkspaceMember> memberships = workspaceMemberRepository.findByUserId(currentUser.getId());
        if (memberships.isEmpty()) {
            throw new ResourceNotFoundException("No workspace found for user");
        }
        
        UUID workspaceId = memberships.get(0).getWorkspace().getId();
        return projectRepository.findByKeyAndWorkspaceId(projectIdentifier.toUpperCase(), workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "key", projectIdentifier));
    }
}
