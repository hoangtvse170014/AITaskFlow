package com.taskflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "is_workspace_role", nullable = false)
    @Builder.Default
    private Boolean isWorkspaceRole = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @JsonIgnore
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static final String OWNER = "OWNER";
    public static final String ADMIN = "ADMIN";
    public static final String MANAGER = "MANAGER";
    public static final String MEMBER = "MEMBER";
    public static final String GUEST = "GUEST";

    public static final int PRIORITY_OWNER = 100;
    public static final int PRIORITY_ADMIN = 80;
    public static final int PRIORITY_MANAGER = 60;
    public static final int PRIORITY_MEMBER = 40;
    public static final int PRIORITY_GUEST = 20;

    public boolean hasPermission(String permissionName) {
        return permissions.stream()
            .anyMatch(p -> p.getName().equals(permissionName));
    }

    public boolean hasAnyPermission(String... permissionNames) {
        for (String name : permissionNames) {
            if (hasPermission(name)) return true;
        }
        return false;
    }
}
