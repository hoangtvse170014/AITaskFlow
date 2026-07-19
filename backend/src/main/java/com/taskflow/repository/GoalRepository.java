package com.taskflow.repository;

import com.taskflow.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GoalRepository extends JpaRepository<Goal, UUID> {

    List<Goal> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    @Query("SELECT g FROM Goal g WHERE g.workspace.id = :workspaceId AND g.parentGoalId IS NULL ORDER BY g.createdAt DESC")
    List<Goal> findTopLevelGoalsByWorkspace(@Param("workspaceId") UUID workspaceId);

    List<Goal> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    @Query("SELECT g FROM Goal g WHERE g.workspace.id = :workspaceId AND g.status = :status ORDER BY g.dueDate ASC")
    List<Goal> findByWorkspaceAndStatus(@Param("workspaceId") UUID workspaceId, @Param("status") Goal.GoalStatus status);

    @Query("SELECT g FROM Goal g WHERE g.workspace.id = :workspaceId AND g.isArchived = false ORDER BY g.progressPercentage ASC")
    List<Goal> findActiveGoalsByWorkspace(@Param("workspaceId") UUID workspaceId);
}
