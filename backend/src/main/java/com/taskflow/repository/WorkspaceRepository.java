package com.taskflow.repository;

import com.taskflow.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    Optional<Workspace> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT w FROM Workspace w JOIN w.members m WHERE m.user.id = :userId")
    List<Workspace> findAllByMemberUserId(@Param("userId") UUID userId);

    @Query("SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END FROM Workspace w JOIN w.members m " +
           "WHERE w.id = :workspaceId AND m.user.id = :userId")
    boolean isUserMemberOfWorkspace(@Param("workspaceId") UUID workspaceId, @Param("userId") UUID userId);

    @Query("SELECT w FROM Workspace w WHERE w.owner.id = :userId")
    List<Workspace> findAllByOwnerId(@Param("userId") UUID userId);
}
