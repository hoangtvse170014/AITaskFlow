package com.taskflow.repository;

import com.taskflow.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {

    @Query("SELECT a FROM ActivityLog a WHERE a.workspace.id = :workspaceId ORDER BY a.createdAt DESC")
    List<ActivityLog> findAllByWorkspaceIdOrderByCreatedAtDesc(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT a FROM ActivityLog a WHERE a.workspace.id = :workspaceId AND a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.createdAt DESC")
    List<ActivityLog> findByEntity(@Param("workspaceId") UUID workspaceId, @Param("entityType") String entityType, @Param("entityId") UUID entityId);

    @Query("SELECT a FROM ActivityLog a WHERE a.user.id = :userId ORDER BY a.createdAt DESC")
    List<ActivityLog> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);
}
