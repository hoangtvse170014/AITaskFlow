package com.taskflow.service;

import com.taskflow.dto.request.CreateWorkspaceRequest;
import com.taskflow.dto.request.InviteMemberRequest;
import com.taskflow.dto.response.WorkspaceMemberResponse;
import com.taskflow.dto.response.WorkspaceResponse;

import java.util.List;
import java.util.UUID;

public interface WorkspaceService {

    List<WorkspaceResponse> getAllWorkspaces();

    WorkspaceResponse getWorkspaceById(UUID workspaceId);

    WorkspaceResponse createWorkspace(CreateWorkspaceRequest request);

    WorkspaceResponse updateWorkspace(UUID workspaceId, CreateWorkspaceRequest request);

    void deleteWorkspace(UUID workspaceId);

    WorkspaceResponse inviteMember(UUID workspaceId, InviteMemberRequest request);

    List<WorkspaceMemberResponse> getWorkspaceMembers(UUID workspaceId);

    void removeMember(UUID workspaceId, UUID memberId);

    boolean isWorkspaceMember(UUID workspaceId, UUID userId);

    boolean isWorkspaceAdmin(UUID workspaceId, UUID userId);

    String getUserRoleInWorkspace(UUID workspaceId, UUID userId);
}
