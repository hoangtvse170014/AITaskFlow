package com.taskflow.repository;

import com.taskflow.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(String name);

    @Query("SELECT r FROM Role r WHERE r.isWorkspaceRole = true")
    Set<Role> findAllWorkspaceRoles();

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.name = :name")
    Optional<Role> findByNameWithPermissions(String name);

    boolean existsByName(String name);
}
