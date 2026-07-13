package com.taskflow.repository;

import com.taskflow.entity.KeyResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KeyResultRepository extends JpaRepository<KeyResult, UUID> {

    List<KeyResult> findByGoalIdOrderByCreatedAtDesc(UUID goalId);

    List<KeyResult> findByAssigneeIdOrderByDueDateAsc(UUID assigneeId);

    @Query("SELECT kr FROM KeyResult kr WHERE kr.goal.id = :goalId ORDER BY kr.progressPercentage ASC")
    List<KeyResult> findByGoalIdOrderByProgress(@Param("goalId") UUID goalId);

    @Query("SELECT kr FROM KeyResult kr WHERE kr.status = :status ORDER BY kr.dueDate ASC")
    List<KeyResult> findByStatus(@Param("status") KeyResult.KeyResultStatus status);
}
