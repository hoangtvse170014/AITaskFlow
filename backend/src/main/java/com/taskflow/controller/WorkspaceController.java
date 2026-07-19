package com.taskflow.controller;

import com.taskflow.dto.request.CreateWorkspaceRequest;
import com.taskflow.dto.request.InviteMemberRequest;
import com.taskflow.dto.response.ApiResponse;
import com.taskflow.dto.response.WorkspaceMemberResponse;
import com.taskflow.dto.response.WorkspaceResponse;
import com.taskflow.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkspaceResponse>>> getAllWorkspaces() {
        List<WorkspaceResponse> workspaces = workspaceService.getAllWorkspaces();
        return ResponseEntity.ok(ApiResponse.success(workspaces));
    }

    @GetMapping("/{workspaceId}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> getWorkspaceById(@PathVariable UUID workspaceId) {
        WorkspaceResponse workspace = workspaceService.getWorkspaceById(workspaceId);
        return ResponseEntity.ok(ApiResponse.success(workspace));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceResponse>> createWorkspace(
            @Valid @RequestBody CreateWorkspaceRequest request) {
        WorkspaceResponse workspace = workspaceService.createWorkspace(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(workspace, "Workspace created successfully"));
    }

    @PutMapping("/{workspaceId}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> updateWorkspace(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateWorkspaceRequest request) {
        WorkspaceResponse workspace = workspaceService.updateWorkspace(workspaceId, request);
        return ResponseEntity.ok(ApiResponse.success(workspace, "Workspace updated successfully"));
    }

    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<ApiResponse<Void>> deleteWorkspace(@PathVariable UUID workspaceId) {
        workspaceService.deleteWorkspace(workspaceId);
        return ResponseEntity.ok(ApiResponse.success(null, "Workspace deleted successfully"));
    }

    @PostMapping("/{workspaceId}/invite")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> inviteMember(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody InviteMemberRequest request) {
        WorkspaceResponse workspace = workspaceService.inviteMember(workspaceId, request);
        return ResponseEntity.ok(ApiResponse.success(workspace, "Member invited successfully"));
    }

    @GetMapping("/{workspaceId}/members")
    public ResponseEntity<ApiResponse<List<WorkspaceMemberResponse>>> getWorkspaceMembers(
            @PathVariable UUID workspaceId) {
        List<WorkspaceMemberResponse> members = workspaceService.getWorkspaceMembers(workspaceId);
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    @DeleteMapping("/{workspaceId}/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID workspaceId,
            @PathVariable UUID memberId) {
        workspaceService.removeMember(workspaceId, memberId);
        return ResponseEntity.ok(ApiResponse.success(null, "Member removed successfully"));
    }
}
