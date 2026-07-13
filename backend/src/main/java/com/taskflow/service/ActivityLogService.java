package com.taskflow.service;

import com.taskflow.entity.*;
import com.taskflow.repository.ActivityLogRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.repository.WorkspaceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public static final String ENTITY_WORKSPACE = "WORKSPACE";
    public static final String ENTITY_PROJECT = "PROJECT";
    public static final String ENTITY_TASK = "TASK";
    public static final String ENTITY_PAGE = "PAGE";
    public static final String ENTITY_MEMBER = "MEMBER";

    public static final String ACTION_CREATED = "CREATED";
    public static final String ACTION_UPDATED = "UPDATED";
    public static final String ACTION_DELETED = "DELETED";
    public static final String ACTION_INVITED = "INVITED";
    public static final String ACTION_REMOVED = "REMOVED";
    public static final String ACTION_ROLE_CHANGED = "ROLE_CHANGED";
    public static final String ACTION_ASSIGNED = "ASSIGNED";
    public static final String ACTION_COMPLETED = "COMPLETED";
    public static final String ACTION_COMMENTED = "COMMENTED";

    @Transactional
    public void logActivity(UUID workspaceId, UUID userId, String action, String entityType, UUID entityId, Object metadata) {
        User user = userRepository.findById(userId).orElse(null);
        Workspace workspace = workspaceRepository.findById(workspaceId).orElse(null);
        
        if (user == null || workspace == null) {
            return;
        }

        String metadataJson = null;
        if (metadata != null) {
            try {
                metadataJson = objectMapper.writeValueAsString(metadata);
            } catch (JsonProcessingException e) {
                metadataJson = metadata.toString();
            }
        }

        ActivityLog log = ActivityLog.builder()
                .workspace(workspace)
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .metadata(metadataJson)
                .build();

        activityLogRepository.save(log);
    }

    @Transactional
    public void logWorkspaceCreated(UUID workspaceId, UUID userId, String workspaceName) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("workspaceName", workspaceName);
        logActivity(workspaceId, userId, ACTION_CREATED, ENTITY_WORKSPACE, workspaceId, metadata);
    }

    @Transactional
    public void logProjectCreated(UUID workspaceId, UUID userId, UUID projectId, String projectName) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("projectName", projectName);
        logActivity(workspaceId, userId, ACTION_CREATED, ENTITY_PROJECT, projectId, metadata);
    }

    @Transactional
    public void logProjectUpdated(UUID workspaceId, UUID userId, UUID projectId, String projectName, Map<String, Object> changes) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("projectName", projectName);
        metadata.put("changes", changes);
        logActivity(workspaceId, userId, ACTION_UPDATED, ENTITY_PROJECT, projectId, metadata);
    }

    @Transactional
    public void logTaskCreated(UUID workspaceId, UUID userId, UUID taskId, String taskTitle, String taskKey) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("taskTitle", taskTitle);
        metadata.put("taskKey", taskKey);
        logActivity(workspaceId, userId, ACTION_CREATED, ENTITY_TASK, taskId, metadata);
    }

    @Transactional
    public void logTaskUpdated(UUID workspaceId, UUID userId, UUID taskId, String taskTitle, Map<String, Object> changes) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("taskTitle", taskTitle);
        metadata.put("changes", changes);
        logActivity(workspaceId, userId, ACTION_UPDATED, ENTITY_TASK, taskId, metadata);
    }

    @Transactional
    public void logTaskDeleted(UUID workspaceId, UUID userId, UUID taskId, String taskTitle) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("taskTitle", taskTitle);
        logActivity(workspaceId, userId, ACTION_DELETED, ENTITY_TASK, taskId, metadata);
    }

    @Transactional
    public void logMemberInvited(UUID workspaceId, UUID userId, UUID invitedUserId, String invitedEmail, String role) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("invitedEmail", invitedEmail);
        metadata.put("role", role);
        logActivity(workspaceId, userId, ACTION_INVITED, ENTITY_MEMBER, invitedUserId, metadata);
    }

    @Transactional
    public void logMemberRoleChanged(UUID workspaceId, UUID userId, UUID memberId, String memberEmail, String oldRole, String newRole) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("memberEmail", memberEmail);
        metadata.put("oldRole", oldRole);
        metadata.put("newRole", newRole);
        logActivity(workspaceId, userId, ACTION_ROLE_CHANGED, ENTITY_MEMBER, memberId, metadata);
    }

    @Transactional
    public void logMemberRemoved(UUID workspaceId, UUID userId, UUID memberId, String memberEmail) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("memberEmail", memberEmail);
        logActivity(workspaceId, userId, ACTION_REMOVED, ENTITY_MEMBER, memberId, metadata);
    }

    public List<ActivityLog> getWorkspaceActivity(UUID workspaceId, int limit) {
        List<ActivityLog> logs = activityLogRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        if (limit > 0 && logs.size() > limit) {
            return logs.subList(0, limit);
        }
        return logs;
    }

    public List<ActivityLog> getEntityActivity(UUID workspaceId, String entityType, UUID entityId) {
        return activityLogRepository.findByEntity(workspaceId, entityType, entityId);
    }

    public List<ActivityLog> getUserActivity(UUID userId, int limit) {
        List<ActivityLog> logs = activityLogRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        if (limit > 0 && logs.size() > limit) {
            return logs.subList(0, limit);
        }
        return logs;
    }
}
