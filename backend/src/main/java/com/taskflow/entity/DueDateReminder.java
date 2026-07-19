package com.taskflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "due_date_reminders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DueDateReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "reminder_date", nullable = false)
    private LocalDate reminderDate;

    @Column(name = "reminder_type", nullable = false, length = 20)
    @Builder.Default
    private String reminderType = "DUE_DATE"; // DUE_DATE, DAY_BEFORE, WEEK_BEFORE

    @Column(name = "is_sent", nullable = false)
    @Builder.Default
    private Boolean isSent = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
