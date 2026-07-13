package com.taskflow.controller;

import com.taskflow.dto.request.SubTaskRequest;
import com.taskflow.dto.response.ApiResponse;
import com.taskflow.dto.response.SubTaskResponse;
import com.taskflow.service.SubTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks/{taskId}/subtasks")
@RequiredArgsConstructor
public class SubTaskController {

    private final SubTaskService subTaskService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubTaskResponse>>> getSubTasks(@PathVariable UUID taskId) {
        List<SubTaskResponse> subTasks = subTaskService.getSubTasks(taskId);
        return ResponseEntity.ok(ApiResponse.success(subTasks));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SubTaskResponse>> createSubTask(
            @PathVariable UUID taskId,
            @Valid @RequestBody SubTaskRequest request) {
        SubTaskResponse subTask = subTaskService.createSubTask(taskId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(subTask, "SubTask created successfully"));
    }

    @PutMapping("/{subTaskId}")
    public ResponseEntity<ApiResponse<SubTaskResponse>> updateSubTask(
            @PathVariable UUID taskId,
            @PathVariable UUID subTaskId,
            @Valid @RequestBody SubTaskRequest request) {
        SubTaskResponse subTask = subTaskService.updateSubTask(taskId, subTaskId, request);
        return ResponseEntity.ok(ApiResponse.success(subTask, "SubTask updated successfully"));
    }

    @PatchMapping("/{subTaskId}/toggle")
    public ResponseEntity<ApiResponse<SubTaskResponse>> toggleSubTask(
            @PathVariable UUID taskId,
            @PathVariable UUID subTaskId) {
        SubTaskResponse subTask = subTaskService.toggleSubTask(taskId, subTaskId);
        return ResponseEntity.ok(ApiResponse.success(subTask));
    }

    @DeleteMapping("/{subTaskId}")
    public ResponseEntity<ApiResponse<Void>> deleteSubTask(
            @PathVariable UUID taskId,
            @PathVariable UUID subTaskId) {
        subTaskService.deleteSubTask(taskId, subTaskId);
        return ResponseEntity.ok(ApiResponse.success(null, "SubTask deleted successfully"));
    }
}
