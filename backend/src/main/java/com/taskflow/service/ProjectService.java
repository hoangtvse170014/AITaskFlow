package com.taskflow.service;

import com.taskflow.dto.request.AddProjectMemberRequest;
import com.taskflow.dto.request.CreateProjectRequest;
import com.taskflow.dto.request.UpdateProjectRequest;
import com.taskflow.dto.response.ProjectMemberResponse;
import com.taskflow.dto.response.ProjectResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ProjectService {

    List<ProjectResponse> getAllProjects(UUID workspaceId);

    ProjectResponse getProjectById(UUID workspaceId, UUID projectId);

    ProjectResponse createProject(UUID workspaceId, CreateProjectRequest request);

    ProjectResponse updateProject(UUID workspaceId, UUID projectId, Map<String, Object> updates);

    void deleteProject(UUID workspaceId, UUID projectId);

    List<ProjectMemberResponse> getProjectMembers(UUID projectId);

    ProjectMemberResponse addProjectMember(UUID projectId, AddProjectMemberRequest request);

    void removeProjectMember(UUID projectId, UUID memberId);

    boolean isProjectMember(UUID projectId, UUID userId);

    boolean isProjectAdmin(UUID projectId, UUID userId);
}
