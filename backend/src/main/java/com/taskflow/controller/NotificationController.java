package com.taskflow.controller;

import com.taskflow.dto.response.ApiResponse;
import com.taskflow.entity.Notification;
import com.taskflow.entity.User;
import com.taskflow.exception.ForbiddenException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNotifications(
            @RequestParam(defaultValue = "20") int limit) {

        User currentUser = getCurrentUser();
        List<Notification> notifications = notificationService.getUserNotifications(currentUser.getId(), limit);
        long unreadCount = notificationService.getUnreadCount(currentUser.getId());

        List<Map<String, Object>> notificationList = notifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("notifications", notificationList);
        response.put("unreadCount", unreadCount);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        User currentUser = getCurrentUser();
        long count = notificationService.getUnreadCount(currentUser.getId());

        Map<String, Long> response = Map.of("count", count);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID notificationId) {
        // SECURITY FIX: Verify ownership before marking as read
        User currentUser = getCurrentUser();
        Notification notification = notificationService.getNotificationById(notificationId);

        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only mark your own notifications as read");
        }

        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        User currentUser = getCurrentUser();
        notificationService.markAllAsRead(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable UUID notificationId) {
        // SECURITY FIX: Verify ownership before deleting
        User currentUser = getCurrentUser();
        Notification notification = notificationService.getNotificationById(notificationId);

        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only delete your own notifications");
        }

        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private Map<String, Object> mapToResponse(Notification notification) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", notification.getId().toString());
        map.put("type", notification.getType());
        map.put("message", notification.getMessage());
        map.put("isRead", notification.getIsRead());
        map.put("createdAt", notification.getCreatedAt() != null ? notification.getCreatedAt().toString() : null);
        return map;
    }
}
