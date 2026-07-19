package com.taskflow.controller;

import com.taskflow.dto.response.ActivityLogResponse;
import com.taskflow.dto.response.ApiResponse;
import com.taskflow.entity.ActivityLog;
import com.taskflow.entity.User;
import com.taskflow.exception.ForbiddenException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.ActivityLogService;
import com.taskflow.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogService activityLogService;
    private final WorkspaceService workspaceService;
    private final UserRepository userRepository;

    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<ApiResponse<List<ActivityLogResponse>>> getWorkspaceActivity(
            @PathVariable UUID workspaceId,
            @RequestParam(defaultValue = "50") int limit) {

        // SECURITY FIX: Verify user is a member of the workspace
        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(workspaceId, currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }

        List<ActivityLog> logs = activityLogService.getWorkspaceActivity(workspaceId, limit);
        List<ActivityLogResponse> response = logs.stream()
                .map(ActivityLogResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
