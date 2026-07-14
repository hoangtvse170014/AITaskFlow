package com.taskflow.repository;

import com.taskflow.entity.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskCommentRepository extends JpaRepository<TaskComment, UUID> {

    List<TaskComment> findAllByTaskIdOrderByCreatedAtAsc(UUID taskId);

    int countByTaskId(UUID taskId);

    /**
     * Returns recent task comments inside the given workspace, newest first.
     * Used by the Workspace Assistant to surface blockers / discussion context
     * in blockers, weekly and daily report answers.
     */
    @Query("SELECT c FROM TaskComment c " +
           "JOIN FETCH c.task t " +
           "JOIN t.project p " +
           "WHERE p.workspace.id = :workspaceId " +
           "ORDER BY c.createdAt DESC")
    List<TaskComment> findRecentByWorkspaceId(@Param("workspaceId") UUID workspaceId);
}
