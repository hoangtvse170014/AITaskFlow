package com.taskflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "key_results", indexes = {
    @Index(name = "idx_key_results_goal", columnList = "goal_id"),
    @Index(name = "idx_key_results_assignee", columnList = "assignee_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeyResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 20)
    @Builder.Default
    private MetricType metricType = MetricType.PERCENTAGE;

    @Column(name = "start_value")
    @Builder.Default
    private Double startValue = 0.0;

    @Column(name = "target_value", nullable = false)
    @Builder.Default
    private Double targetValue = 100.0;

    @Column(name = "current_value")
    @Builder.Default
    private Double currentValue = 0.0;

    @Column(name = "progress_percentage", nullable = false)
    @Builder.Default
    private Integer progressPercentage = 0;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private KeyResultStatus status = KeyResultStatus.ON_TRACK;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum MetricType {
        PERCENTAGE, NUMBER, CURRENCY, BOOLEAN, HOURS
    }

    public enum KeyResultStatus {
        ON_TRACK, AT_RISK, BEHIND, COMPLETED
    }

    public void calculateProgress() {
        if (targetValue == null || targetValue == 0) {
            this.progressPercentage = 0;
            return;
        }
        
        double progress = ((currentValue - startValue) / (targetValue - startValue)) * 100;
        progress = Math.max(0, Math.min(100, progress));
        this.progressPercentage = (int) Math.round(progress);
        
        if (this.progressPercentage >= 100) {
            this.status = KeyResultStatus.COMPLETED;
        } else if (dueDate != null && dueDate.isBefore(java.time.LocalDate.now())) {
            this.status = KeyResultStatus.BEHIND;
        }
    }
}
