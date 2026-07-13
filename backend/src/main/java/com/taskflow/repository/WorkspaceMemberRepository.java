package com.taskflow.repository;

import com.taskflow.entity.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.workspace.id = :workspaceId AND wm.user.id = :userId")
    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);
    
    @Query("SELECT wm FROM WorkspaceMember wm LEFT JOIN FETCH wm.workspace WHERE wm.user.id = :userId")
    List<WorkspaceMember> findByUserId(UUID userId);

    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.workspace.id = :workspaceId AND wm.user.id = :userId AND wm.isActive = true")
    Optional<WorkspaceMember> findActiveByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    @Query("SELECT wm FROM WorkspaceMember wm LEFT JOIN FETCH wm.role LEFT JOIN FETCH wm.user WHERE wm.workspace.id = :workspaceId AND wm.isActive = true")
    List<WorkspaceMember> findAllActiveByWorkspaceId(UUID workspaceId);

    @Query("SELECT wm FROM WorkspaceMember wm LEFT JOIN FETCH wm.role LEFT JOIN FETCH wm.user WHERE wm.workspace.id = :workspaceId")
    List<WorkspaceMember> findAllByWorkspaceId(UUID workspaceId);

    boolean existsByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    @Query("SELECT COUNT(wm) FROM WorkspaceMember wm WHERE wm.workspace.id = :workspaceId AND wm.isActive = true")
    long countActiveMembers(UUID workspaceId);

    @Query("SELECT COUNT(wm) FROM WorkspaceMember wm WHERE wm.workspace.id = :workspaceId")
    long countByWorkspaceId(UUID workspaceId);
}
