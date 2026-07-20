package com.taskflow.service.impl;

import com.taskflow.dto.request.CreateWorkspaceRequest;
import com.taskflow.dto.request.InviteMemberRequest;
import com.taskflow.dto.response.UserResponse;
import com.taskflow.dto.response.WorkspaceMemberResponse;
import com.taskflow.dto.response.WorkspaceResponse;
import com.taskflow.entity.*;
import com.taskflow.exception.BadRequestException;
import com.taskflow.exception.ForbiddenException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.UserRepository;
import com.taskflow.repository.WorkspaceMemberRepository;
import com.taskflow.repository.WorkspaceRepository;
import com.taskflow.repository.RoleRepository;
import com.taskflow.service.ActivityLogService;
import com.taskflow.service.PermissionService;
import com.taskflow.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final RoleRepository roleRepository;
    private final ActivityLogService activityLogService;

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceResponse> getAllWorkspaces() {
        User currentUser = getCurrentUser();
        return workspaceRepository.findAllByMemberUserId(currentUser.getId())
                .stream()
                .map(this::mapToWorkspaceResponse)
                .collect(Collectors.toList());
    }

    @Override
    public WorkspaceResponse getWorkspaceById(UUID workspaceId) {
        User currentUser = getCurrentUser();
        if (!isWorkspaceMember(workspaceId, currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", workspaceId));
        return mapToWorkspaceResponse(workspace);
    }

    @Override
    @Transactional
    public WorkspaceResponse createWorkspace(CreateWorkspaceRequest request) {
        User currentUser = getCurrentUser();

        String slug = generateSlug(request.getName());
        int counter = 1;
        while (workspaceRepository.existsBySlug(slug)) {
            slug = generateSlug(request.getName()) + "-" + counter++;
        }

        Workspace workspace = Workspace.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .owner(currentUser)
                .build();

        workspace = workspaceRepository.save(workspace);

        Role ownerRole = roleRepository.findByNameWithPermissions(Role.OWNER)
                .orElseThrow(() -> new ResourceNotFoundException("Owner role not found"));

        WorkspaceMember ownerMember = WorkspaceMember.builder()
                .workspace(workspace)
                .user(currentUser)
                .role(ownerRole)
                .joinedAt(java.time.LocalDateTime.now())
                .build();

        workspaceMemberRepository.save(ownerMember);

        // Log activity
        activityLogService.logWorkspaceCreated(workspace.getId(), currentUser.getId(), workspace.getName());

        return mapToWorkspaceResponse(workspace);
    }

    @Override
    @Transactional
    public WorkspaceResponse updateWorkspace(UUID workspaceId, CreateWorkspaceRequest request) {
        User currentUser = getCurrentUser();
        permissionService.checkWorkspaceAccess(workspaceId, currentUser.getId(), "workspace:edit", "workspace:manage");

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", workspaceId));

        workspace.setName(request.getName());
        if (request.getDescription() != null) {
            workspace.setDescription(request.getDescription());
        }

        workspace = workspaceRepository.save(workspace);
        return mapToWorkspaceResponse(workspace);
    }

    @Override
    @Transactional
    public void deleteWorkspace(UUID workspaceId) {
        User currentUser = getCurrentUser();
        permissionService.checkWorkspaceAccess(workspaceId, currentUser.getId(), "workspace:delete");

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", workspaceId));

        if (!workspace.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Only the owner can delete this workspace");
        }

        workspaceRepository.delete(workspace);
    }

    @Override
    @Transactional
    public WorkspaceResponse inviteMember(UUID workspaceId, InviteMemberRequest request) {
        User currentUser = getCurrentUser();
        permissionService.checkWorkspaceAccess(workspaceId, currentUser.getId(), "member:invite", "member:manage");

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", workspaceId));

        User invitedUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, invitedUser.getId())) {
            throw new BadRequestException("User is already a member of this workspace");
        }

        Role role = roleRepository.findByNameWithPermissions(request.getRole().toUpperCase())
                .orElseThrow(() -> new BadRequestException("Invalid role: " + request.getRole()));

        WorkspaceMember member = WorkspaceMember.builder()
                .workspace(workspace)
                .user(invitedUser)
                .role(role)
                .joinedAt(java.time.LocalDateTime.now())
                .build();

        workspaceMemberRepository.save(member);

        // Log activity
        activityLogService.logMemberInvited(workspaceId, currentUser.getId(), invitedUser.getId(), invitedUser.getEmail(), role.getName());

        return mapToWorkspaceResponse(workspace);
    }

    @Override
    public List<WorkspaceMemberResponse> getWorkspaceMembers(UUID workspaceId) {
        User currentUser = getCurrentUser();
        permissionService.checkWorkspaceAccess(workspaceId, currentUser.getId(), "member:view");

        return workspaceMemberRepository.findAllByWorkspaceId(workspaceId)
                .stream()
                .map(WorkspaceMemberResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeMember(UUID workspaceId, UUID memberId) {
        User currentUser = getCurrentUser();
        permissionService.checkWorkspaceAccess(workspaceId, currentUser.getId(), "member:remove", "member:manage");

        WorkspaceMember memberToRemove = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", "id", memberId));

        if (memberToRemove.isOwner()) {
            throw new BadRequestException("Cannot remove the workspace owner");
        }

        // Log activity before removal
        activityLogService.logMemberRemoved(workspaceId, currentUser.getId(), memberId, memberToRemove.getUser().getEmail());

        workspaceMemberRepository.delete(memberToRemove);
    }

    @Override
    public boolean isWorkspaceMember(UUID workspaceId, UUID userId) {
        return workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId);
    }

    @Override
    public boolean isWorkspaceAdmin(UUID workspaceId, UUID userId) {
        return permissionService.canManageWorkspace(workspaceId, userId);
    }

    @Override
    public String getUserRoleInWorkspace(UUID workspaceId, UUID userId) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .map(WorkspaceMember::getRole)
                .map(Role::getName)
                .orElse(null);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private WorkspaceResponse mapToWorkspaceResponse(Workspace workspace) {
        User currentUser = getCurrentUser();
        String userRole = getUserRoleInWorkspace(workspace.getId(), currentUser.getId());

        int memberCount = (int) workspaceMemberRepository.countByWorkspaceId(workspace.getId());
        int projectCount = workspace.getProjects() != null ? workspace.getProjects().size() : 0;

        return WorkspaceResponse.builder()
                .id(workspace.getId().toString())
                .workspaceId(workspace.getId().toString())
                .name(workspace.getName())
                .slug(workspace.getSlug())
                .description(workspace.getDescription())
                .owner(UserResponse.fromEntity(workspace.getOwner()))
                .role(userRole)
                .memberCount(memberCount)
                .projectCount(projectCount)
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .build();
    }

    private String generateSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String slug = pattern.matcher(normalized).replaceAll("");
        slug = slug.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return slug;
    }
}
