package com.taskflow.service;

import com.taskflow.entity.Notification;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    Notification createNotification(UUID userId, String type, String message);
    void notifyTaskAssigned(UUID userId, UUID taskId, String taskTitle, String assigneeName);
    void notifyMentioned(UUID userId, UUID taskId, String taskTitle, String mentionerName);
    void notifyComment(UUID userId, UUID taskId, String taskTitle, String commenterName);
    void notifyDeadline(UUID userId, UUID taskId, String taskTitle, int daysLeft);
    void notifyInvitation(UUID userId, String workspaceName, String inviterName);
    void notifyRoleChanged(UUID userId, String workspaceName, String oldRole, String newRole);
    List<Notification> getUserNotifications(UUID userId, int limit);
    Notification getNotificationById(UUID notificationId);
    long getUnreadCount(UUID userId);
    void markAsRead(UUID notificationId);
    void markAllAsRead(UUID userId);
    void deleteNotification(UUID notificationId);
}
