package com.taskflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "member_workloads", indexes = {
    @Index(name = "idx_member_workload_member_date", columnList = "member_id, date"),
    @Index(name = "idx_member_workload_workspace", columnList = "workspace_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberWorkload {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private WorkspaceMember member;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "open_tasks", nullable = false)
    @Builder.Default
    private Integer openTasks = 0;

    @Column(name = "completed_tasks", nullable = false)
    @Builder.Default
    private Integer completedTasks = 0;

    @Column(name = "in_progress_tasks", nullable = false)
    @Builder.Default
    private Integer inProgressTasks = 0;

    @Column(name = "blocked_tasks", nullable = false)
    @Builder.Default
    private Integer blockedTasks = 0;

    @Column(name = "total_hours_estimated")
    private Double totalHoursEstimated;

    @Column(name = "total_hours_logged")
    private Double totalHoursLogged;

    @Column(name = "workload_percentage")
    @Builder.Default
    private Integer workloadPercentage = 0;

    @Column(length = 20)
    @Builder.Default
    private String status = "BALANCED"; // UNDERUTILIZED, BALANCED, OVERLOADED, BLOCKED

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
