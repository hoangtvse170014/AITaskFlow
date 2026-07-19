package com.taskflow.repository;

import com.taskflow.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.project.id = :projectId AND pm.user.id = :userId")
    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

    @Query("SELECT pm FROM ProjectMember pm LEFT JOIN FETCH pm.role LEFT JOIN FETCH pm.user WHERE pm.project.id = :projectId")
    List<ProjectMember> findAllByProjectIdWithDetails(UUID projectId);

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.project.id = :projectId")
    List<ProjectMember> findByProjectId(UUID projectId);

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    @Query("SELECT COUNT(pm) FROM ProjectMember pm WHERE pm.project.id = :projectId")
    long countByProjectId(UUID projectId);
}
