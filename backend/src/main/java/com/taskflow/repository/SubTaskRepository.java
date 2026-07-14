package com.taskflow.repository;

import com.taskflow.entity.SubTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubTaskRepository extends JpaRepository<SubTask, UUID> {

    List<SubTask> findByTaskIdOrderByPositionAsc(UUID taskId);

    void deleteByTaskId(UUID taskId);

    /**
     * Bulk delete every subtask whose parent task belongs to the given project.
     * Used by {@code ProjectServiceImpl.deleteProject} so the FK constraint
     * {@code subtasks.task_id -> tasks.id} does not block the project removal.
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM SubTask s WHERE s.task.id IN " +
           "(SELECT t.id FROM Task t WHERE t.project.id = :projectId)")
    void deleteByProjectId(@org.springframework.data.repository.query.Param("projectId") UUID projectId);

    /**
     * Returns all subtasks for tasks that belong to projects inside the given workspace.
     * Used by the Workspace Assistant to surface in-progress / blocked subtasks in
     * blockers, daily reports and sprint planning answers.
     */
    @Query("SELECT s FROM SubTask s " +
           "JOIN FETCH s.task t " +
           "JOIN t.project p " +
           "WHERE p.workspace.id = :workspaceId " +
           "ORDER BY s.updatedAt DESC")
    List<SubTask> findAllByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT s FROM SubTask s " +
           "JOIN s.task t " +
           "JOIN t.project p " +
           "WHERE p.workspace.id = :workspaceId " +
           "AND s.completed = false")
    List<SubTask> findOpenSubTasksByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT COUNT(s) FROM SubTask s " +
           "JOIN s.task t " +
           "JOIN t.project p " +
           "WHERE p.workspace.id = :workspaceId")
    long countByWorkspaceId(@Param("workspaceId") UUID workspaceId);
}
