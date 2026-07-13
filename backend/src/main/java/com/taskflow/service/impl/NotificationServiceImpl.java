package com.taskflow.service.impl;

import com.taskflow.entity.Notification;
import com.taskflow.entity.User;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.NotificationRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Notification createNotification(UUID userId, String type, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .message(message)
                .isRead(false)
                .build();

        return notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void notifyTaskAssigned(UUID userId, UUID taskId, String taskTitle, String assigneeName) {
        String message = String.format("%s assigned you to task '%s'", assigneeName, taskTitle);
        createNotification(userId, "TASK_ASSIGNED", message);
    }

    @Override
    @Transactional
    public void notifyMentioned(UUID userId, UUID taskId, String taskTitle, String mentionerName) {
        String message = String.format("%s mentioned you in task '%s'", mentionerName, taskTitle);
        createNotification(userId, "MENTION", message);
    }

    @Override
    @Transactional
    public void notifyComment(UUID userId, UUID taskId, String taskTitle, String commenterName) {
        String message = String.format("%s commented on task '%s'", commenterName, taskTitle);
        createNotification(userId, "COMMENT", message);
    }

    @Override
    @Transactional
    public void notifyDeadline(UUID userId, UUID taskId, String taskTitle, int daysLeft) {
        String message = String.format("Task '%s' is due in %d day%s", taskTitle, daysLeft, daysLeft == 1 ? "" : "s");
        createNotification(userId, "DEADLINE", message);
    }

    @Override
    @Transactional
    public void notifyInvitation(UUID userId, String workspaceName, String inviterName) {
        String message = String.format("%s invited you to join '%s'", inviterName, workspaceName);
        createNotification(userId, "INVITATION", message);
    }

    @Override
    @Transactional
    public void notifyRoleChanged(UUID userId, String workspaceName, String oldRole, String newRole) {
        String message = String.format("Your role in '%s' was changed from %s to %s", workspaceName, oldRole, newRole);
        createNotification(userId, "ROLE_CHANGED", message);
    }

    @Override
    public List<Notification> getUserNotifications(UUID userId, int limit) {
        List<Notification> notifications = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        if (limit > 0 && notifications.size() > limit) {
            return new java.util.ArrayList<>(notifications.subList(0, limit)); // Create new list to avoid LazyInitializationException
        }
        return notifications;
    }

    @Override
    public Notification getNotificationById(UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
    }

    @Override
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        List<Notification> notifications = notificationRepository.findAllByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        notifications.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(notifications);
    }

    @Override
    @Transactional
    public void deleteNotification(UUID notificationId) {
        notificationRepository.deleteById(notificationId);
    }
}
