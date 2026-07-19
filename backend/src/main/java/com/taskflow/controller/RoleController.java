package com.taskflow.controller;

import com.taskflow.dto.response.ApiResponse;
import com.taskflow.entity.Role;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleRepository roleRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        List<RoleResponse> roles = roleRepository.findAllWorkspaceRoles()
                .stream()
                .map(RoleResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    @GetMapping("/{roleId}")
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleById(@PathVariable UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));
        return ResponseEntity.ok(ApiResponse.success(RoleResponse.fromEntity(role)));
    }

    public static class RoleResponse {
        public UUID id;
        public String name;
        public String description;
        public Integer priority;
        public Set<String> permissions;

        public static RoleResponse fromEntity(Role role) {
            RoleResponse response = new RoleResponse();
            response.id = role.getId();
            response.name = role.getName();
            response.description = role.getDescription();
            response.priority = role.getPriority();
            response.permissions = role.getPermissions().stream()
                    .map(p -> p.getName())
                    .collect(Collectors.toSet());
            return response;
        }
    }
}
