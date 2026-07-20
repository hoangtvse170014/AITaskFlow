package com.taskflow.controller;

import com.taskflow.entity.Permission;
import com.taskflow.entity.Role;
import com.taskflow.repository.PermissionRepository;
import com.taskflow.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class SeedController {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @PostMapping("/seed/roles")
    public ResponseEntity<?> seedRoles() {
        if (roleRepository.count() > 0) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Roles already exist",
                "roles", roleRepository.findAll().stream().map(r -> r.getName()).toList()
            ));
        }

        Set<Permission> allPermissions = createPermissions();
        
        Role ownerRole = Role.builder()
                .name(Role.OWNER)
                .description("Workspace owner")
                .priority(Role.PRIORITY_OWNER)
                .isWorkspaceRole(true)
                .permissions(allPermissions)
                .build();
        roleRepository.save(ownerRole);

        Role adminRole = Role.builder()
                .name(Role.ADMIN)
                .description("Admin")
                .priority(Role.PRIORITY_ADMIN)
                .isWorkspaceRole(true)
                .permissions(allPermissions)
                .build();
        roleRepository.save(adminRole);

        Role managerRole = Role.builder()
                .name(Role.MANAGER)
                .description("Manager")
                .priority(Role.PRIORITY_MANAGER)
                .isWorkspaceRole(true)
                .permissions(allPermissions)
                .build();
        roleRepository.save(managerRole);

        Role memberRole = Role.builder()
                .name(Role.MEMBER)
                .description("Member")
                .priority(Role.PRIORITY_MEMBER)
                .isWorkspaceRole(true)
                .permissions(allPermissions)
                .build();
        roleRepository.save(memberRole);

        Role guestRole = Role.builder()
                .name(Role.GUEST)
                .description("Guest")
                .priority(Role.PRIORITY_GUEST)
                .isWorkspaceRole(true)
                .permissions(new HashSet<>())
                .build();
        roleRepository.save(guestRole);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Roles seeded successfully",
            "roles", List.of(Role.OWNER, Role.ADMIN, Role.MANAGER, Role.MEMBER, Role.GUEST)
        ));
    }

    private Set<Permission> createPermissions() {
        Set<Permission> permissions = new HashSet<>();
        
        permissions.add(createPermission("workspace:view", "View workspace", Permission.RESOURCE_WORKSPACE, Permission.ACTION_VIEW));
        permissions.add(createPermission("workspace:edit", "Edit workspace", Permission.RESOURCE_WORKSPACE, Permission.ACTION_EDIT));
        permissions.add(createPermission("workspace:manage", "Manage workspace settings", Permission.RESOURCE_WORKSPACE, Permission.ACTION_MANAGE));
        permissions.add(createPermission("member:view", "View members", Permission.RESOURCE_MEMBER, Permission.ACTION_VIEW));
        permissions.add(createPermission("member:invite", "Invite members", Permission.RESOURCE_MEMBER, "INVITE"));
        permissions.add(createPermission("member:manage", "Manage members", Permission.RESOURCE_MEMBER, Permission.ACTION_MANAGE));
        permissions.add(createPermission("project:view", "View projects", Permission.RESOURCE_PROJECT, Permission.ACTION_VIEW));
        permissions.add(createPermission("project:create", "Create projects", Permission.RESOURCE_PROJECT, Permission.ACTION_CREATE));
        permissions.add(createPermission("project:edit", "Edit projects", Permission.RESOURCE_PROJECT, Permission.ACTION_EDIT));
        permissions.add(createPermission("project:manage", "Manage projects", Permission.RESOURCE_PROJECT, Permission.ACTION_MANAGE));
        permissions.add(createPermission("task:view", "View tasks", Permission.RESOURCE_TASK, Permission.ACTION_VIEW));
        permissions.add(createPermission("task:create", "Create tasks", Permission.RESOURCE_TASK, Permission.ACTION_CREATE));
        permissions.add(createPermission("task:edit", "Edit tasks", Permission.RESOURCE_TASK, Permission.ACTION_EDIT));
        permissions.add(createPermission("task:assign", "Assign tasks", Permission.RESOURCE_TASK, "ASSIGN"));
        permissions.add(createPermission("page:view", "View pages", Permission.RESOURCE_PAGE, Permission.ACTION_VIEW));
        permissions.add(createPermission("page:create", "Create pages", Permission.RESOURCE_PAGE, Permission.ACTION_CREATE));
        permissions.add(createPermission("page:edit", "Edit pages", Permission.RESOURCE_PAGE, Permission.ACTION_EDIT));
        
        permissionRepository.saveAll(permissions);
        return permissions;
    }

    private Permission createPermission(String name, String description, String resourceType, String actionType) {
        return Permission.builder()
                .name(name)
                .description(description)
                .resourceType(resourceType)
                .actionType(actionType)
                .build();
    }
}
