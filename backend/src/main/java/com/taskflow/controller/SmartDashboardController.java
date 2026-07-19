package com.taskflow.controller;

import com.taskflow.dto.response.ApiResponse;
import com.taskflow.dto.response.SmartDashboardResponse;
import com.taskflow.entity.User;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.SmartDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard/smart")
@RequiredArgsConstructor
public class SmartDashboardController {

    private final SmartDashboardService smartDashboardService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<SmartDashboardResponse>> getSmartDashboard(
            @RequestParam UUID workspaceId) {
        
        User currentUser = getCurrentUser();
        SmartDashboardResponse dashboard = smartDashboardService.getSmartDashboard(workspaceId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
