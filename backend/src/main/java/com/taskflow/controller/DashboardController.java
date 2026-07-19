package com.taskflow.controller;

import com.taskflow.dto.response.ApiResponse;
import com.taskflow.dto.response.DashboardResponse;
import com.taskflow.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats/{workspaceId}")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboardStats(@PathVariable UUID workspaceId) {
        DashboardResponse stats = dashboardService.getDashboardStats(workspaceId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
