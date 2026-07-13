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
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @ManyToMany(mappedBy = "permissions")
    @JsonIgnore
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static final String RESOURCE_WORKSPACE = "WORKSPACE";
    public static final String RESOURCE_PROJECT = "PROJECT";
    public static final String RESOURCE_TASK = "TASK";
    public static final String RESOURCE_PAGE = "PAGE";
    public static final String RESOURCE_MEMBER = "MEMBER";
    public static final String RESOURCE_SETTINGS = "SETTINGS";
    public static final String RESOURCE_BILLING = "BILLING";

    public static final String ACTION_VIEW = "VIEW";
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_EDIT = "EDIT";
    public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_MANAGE = "MANAGE";
}
