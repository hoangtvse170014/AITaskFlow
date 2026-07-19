package com.taskflow.repository;

import com.taskflow.entity.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PageRepository extends JpaRepository<Page, UUID> {

    List<Page> findAllByWorkspaceIdAndParentIsNullOrderBySidebarOrder(UUID workspaceId);

    List<Page> findAllByWorkspaceIdOrderBySidebarOrder(UUID workspaceId);

    List<Page> findAllByParentIdOrderBySidebarOrder(UUID parentId);

    Optional<Page> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    @Query("SELECT p FROM Page p WHERE p.workspace.id = :workspaceId AND p.isArchived = false ORDER BY p.sidebarOrder")
    List<Page> findAllActivePagesByWorkspace(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT p FROM Page p WHERE p.workspace.id = :workspaceId AND p.parent IS NULL AND p.isArchived = false ORDER BY p.sidebarOrder")
    List<Page> findRootPagesByWorkspace(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT COALESCE(MAX(p.sidebarOrder), 0) FROM Page p WHERE p.workspace.id = :workspaceId AND p.parent IS NULL")
    Integer findMaxSidebarOrderForRootPages(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT COALESCE(MAX(p.sidebarOrder), 0) FROM Page p WHERE p.parent.id = :parentId")
    Integer findMaxSidebarOrderForChildPages(@Param("parentId") UUID parentId);

    @Query("SELECT p FROM Page p WHERE p.workspace.id = :workspaceId AND LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) AND p.isArchived = false")
    List<Page> searchByTitle(@Param("workspaceId") UUID workspaceId, @Param("query") String query);

    @Query("SELECT DISTINCT p FROM Page p JOIN p.blocks b WHERE p.workspace.id = :workspaceId AND LOWER(b.content) LIKE LOWER(CONCAT('%', :query, '%')) AND p.isArchived = false")
    List<Page> searchByContent(@Param("workspaceId") UUID workspaceId, @Param("query") String query);
}
