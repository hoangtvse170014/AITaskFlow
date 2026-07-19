package com.taskflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "task_number", nullable = false)
    private Integer taskNumber;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "labels", columnDefinition = "TEXT")
    private String labelsJson;

    @Column(name = "checklist", columnDefinition = "TEXT")
    private String checklistJson;

    @Transient
    @Getter(AccessLevel.NONE)
    private List<Label> labels;

    
    @Transient
    @Getter(AccessLevel.NONE)
    private List<ChecklistItem> checklist;

    public List<Label> getLabels() {
        if (labels == null && labelsJson != null) {
            try {
                labels = objectMapper.readValue(labelsJson, new TypeReference<List<Label>>() {});
            } catch (JsonProcessingException e) {
                labels = new ArrayList<>();
            }
        }
        return labels != null ? labels : new ArrayList<>();
    }

    public void setLabels(List<Label> labels) {
        this.labels = labels;
        try {
            this.labelsJson = labels != null ? objectMapper.writeValueAsString(labels) : null;
        } catch (JsonProcessingException e) {
            this.labelsJson = "[]";
        }
    }

    public List<ChecklistItem> getChecklist() {
        if (checklist == null && checklistJson != null) {
            try {
                checklist = objectMapper.readValue(checklistJson, new TypeReference<List<ChecklistItem>>() {});
            } catch (JsonProcessingException e) {
                checklist = new ArrayList<>();
            }
        }
        return checklist != null ? checklist : new ArrayList<>();
    }

    public void setChecklist(List<ChecklistItem> checklist) {
        this.checklist = checklist;
        try {
            this.checklistJson = checklist != null ? objectMapper.writeValueAsString(checklist) : null;
        } catch (JsonProcessingException e) {
            this.checklistJson = "[]";
        }
    }

    @Column(nullable = false)
    @Builder.Default
    private Integer position = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<TaskComment> taskComments = new ArrayList<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<TaskActivityLog> taskActivityLogs = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Label {
        private String id;
        private String name;
        private String color;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChecklistItem {
        private String id;
        private String text;
        private boolean completed;
    }

    @Transient
    public String getTaskKey() {
        return project != null ? project.getKey() + "-" + taskNumber : "TASK-" + taskNumber;
    }

    @Transient
    public boolean isOverdue() {
        return dueDate != null && dueDate.isBefore(LocalDate.now()) && status != TaskStatus.DONE;
    }
}
