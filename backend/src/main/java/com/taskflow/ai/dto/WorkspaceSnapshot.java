package com.taskflow.ai.dto;

import com.taskflow.entity.*;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Aggregated snapshot of everything the Workspace Assistant may want to read
 * about a workspace. Built once per request inside a transaction, then handed
 * to the prompt builders and intent-specific services so we never hit the
 * database twice for the same question.
 */
@Data
@Builder
public class WorkspaceSnapshot {

    private Workspace workspace;

    private List<Project> projects;

    private List<Task> tasks;

    private List<SubTask> subtasks;

    private List<Goal> goals;

    private List<Page> pages;

    private List<Block> recentBlocks;

    private List<WorkspaceMember> members;

    private List<MemberWorkload> workloads;

    private List<ActivityLog> recentActivities;

    private List<TaskComment> recentComments;

    /** Cached today so all date math inside the prompt is consistent. */
    private java.time.LocalDate today;

    public UUID getId() {
        return workspace != null ? workspace.getId() : null;
    }
}
