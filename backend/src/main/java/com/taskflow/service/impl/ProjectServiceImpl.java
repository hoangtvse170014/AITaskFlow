package com.taskflow.service.impl;

import com.taskflow.dto.request.AddProjectMemberRequest;
import com.taskflow.dto.request.CreateProjectRequest;
import com.taskflow.dto.response.ProjectMemberResponse;
import com.taskflow.dto.response.ProjectResponse;
import com.taskflow.entity.Project;
import com.taskflow.entity.ProjectMember;
import com.taskflow.entity.Role;
import com.taskflow.entity.Task;
import com.taskflow.entity.User;
import com.taskflow.entity.Workspace;
import com.taskflow.exception.BadRequestException;
import com.taskflow.exception.ForbiddenException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.ProjectMemberRepository;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.RoleRepository;
import com.taskflow.repository.SubTaskRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.repository.WorkspaceRepository;
import com.taskflow.service.ActivityLogService;
import com.taskflow.service.ProjectService;
import com.taskflow.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TaskRepository taskRepository;
    private final SubTaskRepository subTaskRepository;
    private final WorkspaceService workspaceService;
    private final ActivityLogService activityLogService;

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects(UUID workspaceId) {
        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(workspaceId, currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }

        return projectRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                .stream()
                .map(this::mapToProjectResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(UUID workspaceId, UUID projectId) {
        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(workspaceId, currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }

        Project project = projectRepository.findByIdAndWorkspaceId(projectId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        return mapToProjectResponse(project);
    }

    @Override
    @Transactional
    public ProjectResponse createProject(UUID workspaceId, CreateProjectRequest request) {
        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(workspaceId, currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", workspaceId));

        if (projectRepository.existsByWorkspaceIdAndKey(workspaceId, request.getKey().toUpperCase())) {
            throw new BadRequestException("Project key already exists in this workspace");
        }

        Project project = Project.builder()
                .workspace(workspace)
                .name(request.getName())
                .key(request.getKey().toUpperCase())
                .description(request.getDescription())
                .color(request.getColor() != null ? request.getColor() : "#6366f1")
                .icon(request.getIcon())
                .build();

        project = projectRepository.save(project);

        // Log activity
        activityLogService.logProjectCreated(workspaceId, currentUser.getId(), project.getId(), project.getName());

        return mapToProjectResponse(project);
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(UUID workspaceId, UUID projectId, Map<String, Object> updates) {
        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(workspaceId, currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }

        Project project = projectRepository.findByIdAndWorkspaceId(projectId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        if (updates.containsKey("name") && updates.get("name") != null) {
            project.setName((String) updates.get("name"));
        }
        if (updates.containsKey("description")) {
            project.setDescription((String) updates.get("description"));
        }
        if (updates.containsKey("color") && updates.get("color") != null) {
            project.setColor((String) updates.get("color"));
        }
        if (updates.containsKey("icon")) {
            project.setIcon((String) updates.get("icon"));
        }
        if (updates.containsKey("key") && updates.get("key") != null) {
            // Validate new key doesn't conflict
            String newKey = ((String) updates.get("key")).toUpperCase();
            if (!newKey.equals(project.getKey()) && projectRepository.existsByWorkspaceIdAndKey(workspaceId, newKey)) {
                throw new BadRequestException("Project key already exists in this workspace");
            }
            project.setKey(newKey);
        }

        project = projectRepository.save(project);

        // Log activity
        activityLogService.logProjectUpdated(workspaceId, currentUser.getId(), project.getId(), project.getName(), null);

        return mapToProjectResponse(project);
    }

    @Override
    @Transactional
    public void deleteProject(UUID workspaceId, UUID projectId) {
        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(workspaceId, currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }

        Project project = projectRepository.findByIdAndWorkspaceId(projectId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        // Cascade cleanup of children before the project row is dropped.
        // PostgreSQL FK constraints would otherwise reject the DELETE on
        // "tasks" because "subtasks" still reference them.
        subTaskRepository.deleteByProjectId(projectId);
        subTaskRepository.flush();

        List<Task> projectTasks = taskRepository.findAllByProjectIdOrderByPosition(projectId);
        if (!projectTasks.isEmpty()) {
            taskRepository.deleteAll(projectTasks);
            taskRepository.flush();
        }

        projectRepository.delete(project);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getProjectMembers(UUID projectId) {
        return projectMemberRepository.findByProjectId(projectId)
                .stream()
                .map(ProjectMemberResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProjectMemberResponse addProjectMember(UUID projectId, AddProjectMemberRequest request) {
        User currentUser = getCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        if (!workspaceService.isWorkspaceAdmin(project.getWorkspace().getId(), currentUser.getId()) 
            && !isProjectAdmin(projectId, currentUser.getId())) {
            throw new ForbiddenException("Only admins can add members to this project");
        }

        User newMember = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, newMember.getId())) {
            throw new BadRequestException("User is already a member of this project");
        }

        Role memberRole = roleRepository.findByNameWithPermissions(request.getRole() != null ? request.getRole().toUpperCase() : "MEMBER")
                .orElseGet(() -> roleRepository.findByName("MEMBER").orElse(null));

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(newMember)
                .role(memberRole)
                .build();

        member = projectMemberRepository.save(member);
        return ProjectMemberResponse.fromEntity(member);
    }

    @Override
    @Transactional
    public void removeProjectMember(UUID projectId, UUID memberId) {
        User currentUser = getCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        if (!workspaceService.isWorkspaceAdmin(project.getWorkspace().getId(), currentUser.getId()) 
            && !isProjectAdmin(projectId, currentUser.getId())) {
            throw new ForbiddenException("Only admins can remove members from this project");
        }

        ProjectMember member = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectMember", "id", memberId));

        projectMemberRepository.delete(member);
    }

    @Override
    public boolean isProjectMember(UUID projectId, UUID userId) {
        return projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
    }

    @Override
    public boolean isProjectAdmin(UUID projectId, UUID userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .map(member -> member.getRole() != null && 
                    (Role.ADMIN.equals(member.getRole().getName()) || 
                     Role.MANAGER.equals(member.getRole().getName())))
                .orElse(false);
    }

    private ProjectResponse mapToProjectResponse(Project project) {
        long taskCount = taskRepository.countByProjectId(project.getId());
        long completedCount = taskRepository.countByProjectIdAndStatus(project.getId(), com.taskflow.entity.TaskStatus.DONE);

        return ProjectResponse.builder()
                .id(project.getId().toString())
                .workspaceId(project.getWorkspace().getId().toString())
                .name(project.getName())
                .key(project.getKey())
                .description(project.getDescription())
                .color(project.getColor())
                .icon(project.getIcon())
                .taskCount((int) taskCount)
                .completedTaskCount((int) completedCount)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
