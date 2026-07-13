package com.taskflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workspace_members", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"workspace_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @CreationTimestamp
    @Column(name = "invited_at", nullable = false, updatable = false)
    private LocalDateTime invitedAt;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "nickname", length = 100)
    private String nickname;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    public boolean hasPermission(String permissionName) {
        return role != null && role.hasPermission(permissionName);
    }

    public boolean hasAnyPermission(String... permissionNames) {
        return role != null && role.hasAnyPermission(permissionNames);
    }

    public boolean isOwner() {
        return role != null && Role.OWNER.equals(role.getName());
    }

    public boolean isAdmin() {
        return role != null && (Role.ADMIN.equals(role.getName()) || Role.OWNER.equals(role.getName()));
    }

    public boolean canManage() {
        return role != null && role.getPriority() >= Role.PRIORITY_MANAGER;
    }
}
