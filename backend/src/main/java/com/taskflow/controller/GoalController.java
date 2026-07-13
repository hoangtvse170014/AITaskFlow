package com.taskflow.controller;

import com.taskflow.dto.request.CreateGoalRequest;
import com.taskflow.dto.request.CreateKeyResultRequest;
import com.taskflow.dto.response.ApiResponse;
import com.taskflow.dto.response.GoalResponse;
import com.taskflow.entity.User;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;
    private final UserRepository userRepository;

    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<ApiResponse<List<GoalResponse>>> getWorkspaceGoals(
            @PathVariable UUID workspaceId) {
        List<GoalResponse> goals = goalService.getWorkspaceGoals(workspaceId);
        return ResponseEntity.ok(ApiResponse.success(goals));
    }

    @GetMapping("/workspace/{workspaceId}/my")
    public ResponseEntity<ApiResponse<List<GoalResponse>>> getMyGoals(
            @PathVariable UUID workspaceId) {
        User currentUser = getCurrentUser();
        List<GoalResponse> goals = goalService.getMyGoals(workspaceId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(goals));
    }

    @GetMapping("/{goalId}")
    public ResponseEntity<ApiResponse<GoalResponse>> getGoal(@PathVariable UUID goalId) {
        GoalResponse goal = goalService.getGoalById(goalId);
        return ResponseEntity.ok(ApiResponse.success(goal));
    }

    @PostMapping("/workspace/{workspaceId}")
    public ResponseEntity<ApiResponse<GoalResponse>> createGoal(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateGoalRequest request) {
        GoalResponse goal = goalService.createGoal(workspaceId, request);
        return ResponseEntity.ok(ApiResponse.success(goal));
    }

    @PutMapping("/{goalId}")
    public ResponseEntity<ApiResponse<GoalResponse>> updateGoal(
            @PathVariable UUID goalId,
            @Valid @RequestBody CreateGoalRequest request) {
        GoalResponse goal = goalService.updateGoal(goalId, request);
        return ResponseEntity.ok(ApiResponse.success(goal));
    }

    @DeleteMapping("/{goalId}")
    public ResponseEntity<ApiResponse<Void>> deleteGoal(@PathVariable UUID goalId) {
        goalService.deleteGoal(goalId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{goalId}/key-results")
    public ResponseEntity<ApiResponse<GoalResponse>> addKeyResult(
            @PathVariable UUID goalId,
            @Valid @RequestBody CreateKeyResultRequest request) {
        GoalResponse goal = goalService.addKeyResult(goalId, request);
        return ResponseEntity.ok(ApiResponse.success(goal));
    }

    @PatchMapping("/{goalId}/key-results/{keyResultId}")
    public ResponseEntity<ApiResponse<GoalResponse>> updateKeyResult(
            @PathVariable UUID goalId,
            @PathVariable UUID keyResultId,
            @RequestParam double currentValue) {
        GoalResponse goal = goalService.updateKeyResult(keyResultId, currentValue);
        return ResponseEntity.ok(ApiResponse.success(goal));
    }

    @DeleteMapping("/{goalId}/key-results/{keyResultId}")
    public ResponseEntity<ApiResponse<Void>> deleteKeyResult(
            @PathVariable UUID goalId,
            @PathVariable UUID keyResultId) {
        goalService.deleteKeyResult(keyResultId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{goalId}/archive")
    public ResponseEntity<ApiResponse<GoalResponse>> archiveGoal(@PathVariable UUID goalId) {
        GoalResponse goal = goalService.archiveGoal(goalId);
        return ResponseEntity.ok(ApiResponse.success(goal));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
