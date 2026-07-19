package com.taskflow.controller;

import com.taskflow.dto.request.CreateCommentRequest;
import com.taskflow.dto.request.CreateTaskRequest;
import com.taskflow.dto.request.UpdateTaskPositionRequest;
import com.taskflow.dto.request.UpdateTaskRequest;
import com.taskflow.dto.response.ApiResponse;
import com.taskflow.dto.response.TaskResponse;
import com.taskflow.dto.response.TaskActivityLogResponse;
import com.taskflow.dto.response.TaskCommentResponse;
import com.taskflow.entity.Project;
import com.taskflow.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/api/projects/{projectId}/tasks")
    public ResponseEntity<ApiResponse<Map<String, List<TaskResponse>>>> getTasksByStatus(@PathVariable String projectId) {
        Project project = taskService.resolveProjectIdentifier(projectId);
        Map<String, List<TaskResponse>> tasks = taskService.getTasksByStatus(project.getId());
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @GetMapping("/api/projects/{projectId}/tasks/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTaskById(
            @PathVariable String projectId,
            @PathVariable UUID taskId) {
        Project project = taskService.resolveProjectIdentifier(projectId);
        TaskResponse task = taskService.getTaskById(project.getId(), taskId);
        return ResponseEntity.ok(ApiResponse.success(task));
    }

    @PostMapping("/api/projects/{projectId}/tasks")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @PathVariable String projectId,
            @Valid @RequestBody CreateTaskRequest request) {
        Project project = taskService.resolveProjectIdentifier(projectId);
        TaskResponse task = taskService.createTask(project.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(task, "Task created successfully"));
    }

    @PutMapping("/api/projects/{projectId}/tasks/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable String projectId,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        Project project = taskService.resolveProjectIdentifier(projectId);
        TaskResponse task = taskService.updateTask(project.getId(), taskId, request);
        return ResponseEntity.ok(ApiResponse.success(task, "Task updated successfully"));
    }

    @PatchMapping("/api/projects/{projectId}/tasks/{taskId}/position")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTaskPosition(
            @PathVariable String projectId,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskPositionRequest request) {
        Project project = taskService.resolveProjectIdentifier(projectId);
        TaskResponse task = taskService.updateTaskPosition(project.getId(), taskId, request);
        return ResponseEntity.ok(ApiResponse.success(task));
    }

    @DeleteMapping("/api/projects/{projectId}/tasks/{taskId}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @PathVariable String projectId,
            @PathVariable UUID taskId) {
        Project project = taskService.resolveProjectIdentifier(projectId);
        taskService.deleteTask(project.getId(), taskId);
        return ResponseEntity.ok(ApiResponse.success(null, "Task deleted successfully"));
    }

    @GetMapping("/api/tasks/{taskId}/comments")
    public ResponseEntity<ApiResponse<List<TaskCommentResponse>>> getTaskComments(@PathVariable UUID taskId) {
        List<TaskCommentResponse> comments = taskService.getTaskComments(taskId);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    @PostMapping("/api/tasks/{taskId}/comments")
    public ResponseEntity<ApiResponse<TaskCommentResponse>> addComment(
            @PathVariable UUID taskId,
            @Valid @RequestBody CreateCommentRequest request) {
        TaskCommentResponse comment = taskService.addComment(taskId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(comment, "Comment added successfully"));
    }

    @DeleteMapping("/api/tasks/{taskId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable UUID taskId,
            @PathVariable UUID commentId) {
        taskService.deleteComment(taskId, commentId);
        return ResponseEntity.ok(ApiResponse.success(null, "Comment deleted successfully"));
    }

    @GetMapping("/api/tasks/{taskId}/activity")
    public ResponseEntity<ApiResponse<List<TaskActivityLogResponse>>> getTaskActivityLogs(@PathVariable UUID taskId) {
        List<TaskActivityLogResponse> activityLogs = taskService.getTaskActivityLogs(taskId);
        return ResponseEntity.ok(ApiResponse.success(activityLogs));
    }
}
