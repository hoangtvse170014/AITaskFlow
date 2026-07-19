package com.taskflow.controller;

import com.taskflow.dto.response.ApiResponse;
import com.taskflow.dto.response.WorkloadResponse;
import com.taskflow.entity.User;
import com.taskflow.exception.ForbiddenException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.WorkspaceService;
import com.taskflow.service.WorkloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/workloads")
@RequiredArgsConstructor
public class WorkloadController {

    private final WorkloadService workloadService;
    private final WorkspaceService workspaceService;
    private final UserRepository userRepository;

    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<ApiResponse<WorkloadResponse>> getWorkloadOverview(
            @PathVariable UUID workspaceId) {

        // SECURITY FIX: Verify user is a member of the workspace
        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(workspaceId, currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }

        WorkloadResponse response = workloadService.getWorkloadOverview(workspaceId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/workspace/{workspaceId}/recalculate")
    public ResponseEntity<ApiResponse<Void>> recalculateWorkloads(
            @PathVariable UUID workspaceId) {

        // SECURITY FIX: Verify user is a member of the workspace
        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(workspaceId, currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }

        workloadService.recalculateWorkloads(workspaceId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
