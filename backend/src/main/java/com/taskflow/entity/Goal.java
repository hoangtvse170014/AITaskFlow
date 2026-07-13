package com.taskflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "goals", indexes = {
    @Index(name = "idx_goals_workspace_parent", columnList = "workspace_id, parent_goal_id"),
    @Index(name = "idx_goals_owner", columnList = "owner_id"),
    @Index(name = "idx_goals_due_date", columnList = "due_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "parent_goal_id")
    private UUID parentGoalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GoalType type = GoalType.TEAM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GoalStatus status = GoalStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GoalPeriod period = GoalPeriod.QUARTERLY;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "target_value")
    @Builder.Default
    private Integer targetValue = 100;

    @Column(name = "current_value")
    @Builder.Default
    private Integer currentValue = 0;

    @Column(name = "progress_percentage", nullable = false)
    @Builder.Default
    private Integer progressPercentage = 0;

    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<KeyResult> keyResults = new ArrayList<>();

    @Column(name = "is_archived", nullable = false)
    @Builder.Default
    private Boolean isArchived = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum GoalType {
        COMPANY, TEAM, PERSONAL
    }

    public enum GoalStatus {
        DRAFT, ACTIVE, ACHIEVED, CANCELLED
    }

    public enum GoalPeriod {
        WEEKLY, MONTHLY, QUARTERLY, YEARLY
    }

    public void calculateProgress() {
        if (keyResults == null || keyResults.isEmpty()) {
            this.progressPercentage = 0;
            return;
        }
        
        int totalProgress = keyResults.stream()
                .mapToInt(kr -> kr.getProgressPercentage() != null ? kr.getProgressPercentage() : 0)
                .sum();
        
        this.progressPercentage = totalProgress / keyResults.size();
        this.currentValue = this.progressPercentage;
    }
}
