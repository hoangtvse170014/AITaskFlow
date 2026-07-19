package com.taskflow.repository;

import com.taskflow.entity.Task;
import com.taskflow.entity.TaskPriority;
import com.taskflow.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findAllByProjectIdOrderByPosition(UUID projectId);

    List<Task> findAllByProjectIdAndStatusOrderByPosition(UUID projectId, TaskStatus status);

    Optional<Task> findByIdAndProjectId(UUID id, UUID projectId);

    @Query("SELECT COALESCE(MAX(t.taskNumber), 0) FROM Task t WHERE t.project.id = :projectId")
    Integer findMaxTaskNumberByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId ORDER BY t.createdAt DESC")
    List<Task> findRecentByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT t FROM Task t WHERE t.assignee.id = :assigneeId ORDER BY t.updatedAt DESC")
    List<Task> findByAssigneeIdOrderByUpdatedAtDesc(@Param("assigneeId") UUID assigneeId);

    @Query("SELECT t FROM Task t WHERE t.dueDate < :date AND t.status != :doneStatus ORDER BY t.dueDate ASC")
    List<Task> findOverdueTasks(@Param("date") LocalDate date, @Param("doneStatus") TaskStatus doneStatus);

    @Query("SELECT t FROM Task t JOIN t.project p JOIN p.workspace w " +
           "WHERE w.id = :workspaceId AND t.dueDate < :date AND t.status != :doneStatus " +
           "ORDER BY t.dueDate ASC")
    List<Task> findOverdueTasksByWorkspaceId(@Param("workspaceId") UUID workspaceId,
                                              @Param("date") LocalDate date,
                                              @Param("doneStatus") TaskStatus doneStatus);

    @Query("SELECT t FROM Task t JOIN t.project p JOIN p.workspace w " +
           "WHERE w.id = :workspaceId ORDER BY t.updatedAt DESC")
    List<Task> findAllByWorkspaceIdOrderByUpdatedAtDesc(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId")
    int countByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId AND t.status = :status")
    int countByProjectIdAndStatus(@Param("projectId") UUID projectId, @Param("status") TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t JOIN t.project p WHERE p.workspace.id = :workspaceId")
    int countByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT COUNT(t) FROM Task t JOIN t.project p WHERE p.workspace.id = :workspaceId AND t.status = :status")
    int countByWorkspaceIdAndStatus(@Param("workspaceId") UUID workspaceId, @Param("status") TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t JOIN t.project p WHERE p.workspace.id = :workspaceId " +
           "AND t.dueDate < :today AND t.status != :doneStatus")
    int countOverdueByWorkspaceId(@Param("workspaceId") UUID workspaceId, @Param("today") LocalDate today, @Param("doneStatus") TaskStatus doneStatus);

    @Modifying
    @Query("UPDATE Task t SET t.position = :position WHERE t.id = :taskId")
    void updatePosition(@Param("taskId") UUID taskId, @Param("position") Integer position);

    // Custom count methods for DataInitializer
    long countByStatus(TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.dueDate < :today AND t.status != :doneStatus")
    long countByOverdue(@Param("today") LocalDate today, @Param("doneStatus") TaskStatus doneStatus);

    // ============= Smart Dashboard optimized queries =============

    /**
     * Single workspace-scoped query used by the smart dashboard so we don't
     * pull every task in the database into memory just to filter in Java.
     */
    @Query("SELECT t FROM Task t " +
           "JOIN FETCH t.project p " +
           "LEFT JOIN FETCH t.assignee a " +
           "WHERE p.workspace.id = :workspaceId")
    List<Task> findAllByWorkspaceIdEager(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT t FROM Task t " +
           "JOIN t.project p " +
           "WHERE p.workspace.id = :workspaceId " +
           "AND t.assignee.id = :userId")
    List<Task> findByWorkspaceIdAndAssigneeId(@Param("workspaceId") UUID workspaceId,
                                              @Param("userId") UUID userId);

    @Query("SELECT t FROM Task t " +
           "JOIN t.project p " +
           "WHERE p.workspace.id = :workspaceId " +
           "AND t.assignee.id = :userId " +
           "AND t.status != :doneStatus")
    List<Task> findOpenTasksByWorkspaceAndAssignee(@Param("workspaceId") UUID workspaceId,
                                                   @Param("userId") UUID userId,
                                                   @Param("doneStatus") TaskStatus doneStatus);

    @Query("SELECT t FROM Task t " +
           "JOIN t.project p " +
           "WHERE p.workspace.id = :workspaceId " +
           "AND t.assignee.id = :userId " +
           "AND t.dueDate IS NOT NULL " +
           "AND t.dueDate < :today " +
           "AND t.status != :doneStatus")
    List<Task> findOverdueByWorkspaceAndAssignee(@Param("workspaceId") UUID workspaceId,
                                                 @Param("userId") UUID userId,
                                                 @Param("today") LocalDate today,
                                                 @Param("doneStatus") TaskStatus doneStatus);

    @Query("SELECT t FROM Task t " +
           "JOIN t.project p " +
           "WHERE p.workspace.id = :workspaceId " +
           "AND t.assignee IS NULL " +
           "AND t.status != :doneStatus")
    List<Task> findUnassignedOpenTasksByWorkspaceId(@Param("workspaceId") UUID workspaceId,
                                                    @Param("doneStatus") TaskStatus doneStatus);

    @Query("SELECT t FROM Task t " +
           "JOIN t.project p " +
           "WHERE p.workspace.id = :workspaceId " +
           "AND t.dueDate IS NOT NULL " +
           "AND t.dueDate BETWEEN :startDate AND :endDate " +
           "AND t.status != :doneStatus " +
           "ORDER BY t.dueDate ASC")
    List<Task> findUpcomingByWorkspace(@Param("workspaceId") UUID workspaceId,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate,
                                       @Param("doneStatus") TaskStatus doneStatus);

    @Query("SELECT t FROM Task t " +
           "JOIN t.project p " +
           "WHERE p.workspace.id = :workspaceId " +
           "AND t.assignee.id = :assigneeId")
    List<Task> findAllByWorkspaceAndAssignee(@Param("workspaceId") UUID workspaceId,
                                             @Param("assigneeId") UUID assigneeId);

    @Query("SELECT t FROM Task t " +
           "JOIN t.project p " +
           "WHERE p.workspace.id = :workspaceId " +
           "AND t.createdAt BETWEEN :start AND :end")
    List<Task> findByWorkspaceCreatedBetween(@Param("workspaceId") UUID workspaceId,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    @Query("SELECT t FROM Task t " +
           "JOIN t.project p " +
           "WHERE p.workspace.id = :workspaceId " +
           "AND t.status = :doneStatus " +
           "AND t.updatedAt BETWEEN :start AND :end")
    List<Task> findByWorkspaceCompletedBetween(@Param("workspaceId") UUID workspaceId,
                                               @Param("doneStatus") TaskStatus doneStatus,
                                               @Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);

    // Aggregate projection: returns rows of [status, count]
    @Query("SELECT t.status, COUNT(t) FROM Task t " +
           "JOIN t.project p " +
           "WHERE p.workspace.id = :workspaceId " +
           "GROUP BY t.status")
    List<Object[]> countByWorkspaceGroupByStatus(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT t.priority, COUNT(t) FROM Task t " +
           "JOIN t.project p " +
           "WHERE p.workspace.id = :workspaceId " +
           "GROUP BY t.priority")
    List<Object[]> countByWorkspaceGroupByPriority(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT COUNT(t) FROM Task t " +
           "JOIN t.project p " +
           "WHERE p.workspace.id = :workspaceId " +
           "AND t.dueDate IS NOT NULL " +
           "AND t.dueDate < :today " +
           "AND t.status != :doneStatus")
    int countOverdueInWorkspace(@Param("workspaceId") UUID workspaceId,
                                @Param("today") LocalDate today,
                                @Param("doneStatus") TaskStatus doneStatus);
}