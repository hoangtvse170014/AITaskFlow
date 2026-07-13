package com.taskflow.service;

import com.taskflow.dto.request.CreateCommentRequest;
import com.taskflow.dto.request.CreateTaskRequest;
import com.taskflow.dto.request.UpdateTaskPositionRequest;
import com.taskflow.dto.request.UpdateTaskRequest;
import com.taskflow.dto.response.TaskResponse;
import com.taskflow.dto.response.TaskActivityLogResponse;
import com.taskflow.dto.response.TaskCommentResponse;
import com.taskflow.entity.Project;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface TaskService {

    Map<String, List<TaskResponse>> getTasksByStatus(UUID projectId);

    TaskResponse getTaskById(UUID projectId, UUID taskId);

    TaskResponse createTask(UUID projectId, CreateTaskRequest request);

    TaskResponse updateTask(UUID projectId, UUID taskId, UpdateTaskRequest request);

    TaskResponse updateTaskPosition(UUID projectId, UUID taskId, UpdateTaskPositionRequest request);

    void deleteTask(UUID projectId, UUID taskId);

    List<TaskCommentResponse> getTaskComments(UUID taskId);

    TaskCommentResponse addComment(UUID taskId, CreateCommentRequest request);

    void deleteComment(UUID taskId, UUID commentId);

    List<TaskActivityLogResponse> getTaskActivityLogs(UUID taskId);
    
    Project resolveProjectIdentifier(String projectIdentifier);
}
