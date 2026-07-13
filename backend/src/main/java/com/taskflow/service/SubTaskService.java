package com.taskflow.service;

import com.taskflow.dto.request.SubTaskRequest;
import com.taskflow.dto.response.SubTaskResponse;

import java.util.List;
import java.util.UUID;

public interface SubTaskService {

    List<SubTaskResponse> getSubTasks(UUID taskId);

    SubTaskResponse createSubTask(UUID taskId, SubTaskRequest request);

    SubTaskResponse updateSubTask(UUID taskId, UUID subTaskId, SubTaskRequest request);

    SubTaskResponse toggleSubTask(UUID taskId, UUID subTaskId);

    void deleteSubTask(UUID taskId, UUID subTaskId);
}
