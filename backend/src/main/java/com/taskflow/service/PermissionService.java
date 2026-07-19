package com.taskflow.service;

import com.taskflow.entity.*;
import com.taskflow.exception.ForbiddenException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final RoleRepository roleRepository;

    public WorkspaceMember getWorkspaceMembership(UUID workspaceId, UUID userId) {
        return workspaceMemberRepository.findActiveByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace membership not found"));
    }

    public void checkWorkspaceAccess(UUID workspaceId, UUID userId, String... permissions) {
        WorkspaceMember membership = getWorkspaceMembership(workspaceId, userId);
        for (String permission : permissions) {
            if (!membership.hasPermission(permission)) {
                throw new ForbiddenException("You don't have permission: " + permission);
            }
        }
    }

    public boolean hasWorkspacePermission(UUID workspaceId, UUID userId, String permission) {
        return workspaceMemberRepository.findActiveByWorkspaceIdAndUserId(workspaceId, userId)
                .map(m -> m.hasPermission(permission))
                .orElse(false);
    }

    public boolean canManageWorkspace(UUID workspaceId, UUID userId) {
        return workspaceMemberRepository.findActiveByWorkspaceIdAndUserId(workspaceId, userId)
                .map(WorkspaceMember::canManage)
                .orElse(false);
    }

    public boolean canManageMembers(UUID workspaceId, UUID userId) {
        return workspaceMemberRepository.findActiveByWorkspaceIdAndUserId(workspaceId, userId)
                .map(m -> m.hasAnyPermission("member:manage", "member:invite"))
                .orElse(false);
    }

    public boolean isWorkspaceOwner(UUID workspaceId, UUID userId) {
        return workspaceMemberRepository.findActiveByWorkspaceIdAndUserId(workspaceId, userId)
                .map(WorkspaceMember::isOwner)
                .orElse(false);
    }

    public ProjectMember getProjectMembership(UUID projectId, UUID userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project membership not found"));
    }

    public void checkProjectAccess(UUID projectId, UUID userId, String... permissions) {
        ProjectMember membership = getProjectMembership(projectId, userId);
        for (String permission : permissions) {
            if (!membership.hasPermission(permission)) {
                throw new ForbiddenException("You don't have project permission: " + permission);
            }
        }
    }

    public boolean hasProjectPermission(UUID projectId, UUID userId, String permission) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .map(m -> m.hasPermission(permission))
                .orElse(false);
    }

    public Role getRoleByName(String roleName) {
        return roleRepository.findByNameWithPermissions(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
    }
}
