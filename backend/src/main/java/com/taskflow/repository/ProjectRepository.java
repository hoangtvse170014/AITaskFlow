package com.taskflow.repository;

import com.taskflow.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findAllByWorkspaceId(UUID workspaceId);

    Optional<Project> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
    
    Optional<Project> findByKeyAndWorkspaceId(String key, UUID workspaceId);

    boolean existsByWorkspaceIdAndKey(UUID workspaceId, String key);

    @Query("SELECT p FROM Project p WHERE p.workspace.id = :workspaceId ORDER BY p.createdAt DESC")
    List<Project> findAllByWorkspaceIdOrderByCreatedAtDesc(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.workspace.id = :workspaceId")
    int countByWorkspaceId(@Param("workspaceId") UUID workspaceId);
}
