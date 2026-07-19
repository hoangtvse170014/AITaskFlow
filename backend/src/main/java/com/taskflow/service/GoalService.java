package com.taskflow.service;

import com.taskflow.dto.request.CreateGoalRequest;
import com.taskflow.dto.request.CreateKeyResultRequest;
import com.taskflow.dto.response.GoalResponse;
import com.taskflow.entity.*;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final KeyResultRepository keyResultRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;

    public List<GoalResponse> getWorkspaceGoals(UUID workspaceId) {
        List<Goal> goals = goalRepository.findTopLevelGoalsByWorkspace(workspaceId);
        return goals.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<GoalResponse> getMyGoals(UUID workspaceId, UUID userId) {
        List<Goal> goals = goalRepository.findByOwnerIdOrderByCreatedAtDesc(userId);
        return goals.stream()
                .filter(g -> g.getWorkspace().getId().equals(workspaceId))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public GoalResponse getGoalById(UUID goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", "id", goalId));
        return mapToResponse(goal);
    }

    @Transactional
    public GoalResponse createGoal(UUID workspaceId, CreateGoalRequest request) {
        User owner = getCurrentUser();
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", workspaceId));

        Goal goal = Goal.builder()
                .workspace(workspace)
                .owner(owner)
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getType() != null ? request.getType() : Goal.GoalType.TEAM)
                .status(Goal.GoalStatus.ACTIVE)
                .period(request.getPeriod() != null ? request.getPeriod() : Goal.GoalPeriod.QUARTERLY)
                .startDate(request.getStartDate())
                .dueDate(request.getDueDate())
                .targetValue(request.getTargetValue() != null ? request.getTargetValue() : 100)
                .build();

        goal = goalRepository.save(goal);
        return mapToResponse(goal);
    }

    @Transactional
    public GoalResponse updateGoal(UUID goalId, CreateGoalRequest request) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", "id", goalId));

        if (request.getTitle() != null) goal.setTitle(request.getTitle());
        if (request.getDescription() != null) goal.setDescription(request.getDescription());
        if (request.getType() != null) goal.setType(request.getType());
        if (request.getStatus() != null) goal.setStatus(request.getStatus());
        if (request.getPeriod() != null) goal.setPeriod(request.getPeriod());
        if (request.getStartDate() != null) goal.setStartDate(request.getStartDate());
        if (request.getDueDate() != null) goal.setDueDate(request.getDueDate());
        if (request.getTargetValue() != null) goal.setTargetValue(request.getTargetValue());

        goal = goalRepository.save(goal);
        return mapToResponse(goal);
    }

    @Transactional
    public void deleteGoal(UUID goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", "id", goalId));
        goalRepository.delete(goal);
    }

    @Transactional
    public GoalResponse addKeyResult(UUID goalId, CreateKeyResultRequest request) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", "id", goalId));

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId()).orElse(null);
        }

        KeyResult keyResult = KeyResult.builder()
                .goal(goal)
                .assignee(assignee)
                .title(request.getTitle())
                .metricType(request.getMetricType() != null ? request.getMetricType() : KeyResult.MetricType.PERCENTAGE)
                .startValue(request.getStartValue() != null ? request.getStartValue() : 0.0)
                .targetValue(request.getTargetValue() != null ? request.getTargetValue() : 100.0)
                .currentValue(request.getStartValue() != null ? request.getStartValue() : 0.0)
                .dueDate(request.getDueDate())
                .build();

        keyResult.calculateProgress();
        keyResultRepository.save(keyResult);

        goal.getKeyResults().add(keyResult);
        goal.calculateProgress();
        goal = goalRepository.save(goal);

        return mapToResponse(goal);
    }

    @Transactional
    public GoalResponse updateKeyResult(UUID keyResultId, double currentValue) {
        KeyResult keyResult = keyResultRepository.findById(keyResultId)
                .orElseThrow(() -> new ResourceNotFoundException("KeyResult", "id", keyResultId));

        keyResult.setCurrentValue(currentValue);
        keyResult.calculateProgress();
        keyResultRepository.save(keyResult);

        Goal goal = keyResult.getGoal();
        goal.calculateProgress();
        goal = goalRepository.save(goal);

        return mapToResponse(goal);
    }

    @Transactional
    public void deleteKeyResult(UUID keyResultId) {
        KeyResult keyResult = keyResultRepository.findById(keyResultId)
                .orElseThrow(() -> new ResourceNotFoundException("KeyResult", "id", keyResultId));

        Goal goal = keyResult.getGoal();
        goal.getKeyResults().remove(keyResult);
        goal.calculateProgress();
        goalRepository.save(goal);

        keyResultRepository.delete(keyResult);
    }

    @Transactional
    public GoalResponse archiveGoal(UUID goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", "id", goalId));
        goal.setIsArchived(true);
        goal = goalRepository.save(goal);
        return mapToResponse(goal);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private GoalResponse mapToResponse(Goal goal) {
        List<GoalResponse.KeyResultResponse> keyResultResponses = goal.getKeyResults().stream()
                .map(kr -> GoalResponse.KeyResultResponse.builder()
                        .id(kr.getId().toString())
                        .title(kr.getTitle())
                        .metricType(kr.getMetricType().name())
                        .startValue(kr.getStartValue())
                        .targetValue(kr.getTargetValue())
                        .currentValue(kr.getCurrentValue())
                        .progressPercentage(kr.getProgressPercentage())
                        .dueDate(kr.getDueDate() != null ? kr.getDueDate().toString() : null)
                        .status(kr.getStatus().name())
                        .assigneeName(kr.getAssignee() != null ? kr.getAssignee().getFullName() : null)
                        .build())
                .collect(Collectors.toList());

        return GoalResponse.builder()
                .id(goal.getId().toString())
                .workspaceId(goal.getWorkspace().getId().toString())
                .ownerId(goal.getOwner().getId().toString())
                .ownerName(goal.getOwner().getFullName())
                .title(goal.getTitle())
                .description(goal.getDescription())
                .type(goal.getType().name())
                .status(goal.getStatus().name())
                .period(goal.getPeriod().name())
                .startDate(goal.getStartDate() != null ? goal.getStartDate().toString() : null)
                .dueDate(goal.getDueDate() != null ? goal.getDueDate().toString() : null)
                .targetValue(goal.getTargetValue())
                .currentValue(goal.getCurrentValue())
                .progressPercentage(goal.getProgressPercentage())
                .keyResults(keyResultResponses)
                .isArchived(goal.getIsArchived())
                .createdAt(goal.getCreatedAt() != null ? goal.getCreatedAt().toString() : null)
                .build();
    }
}
