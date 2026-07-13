package com.taskflow.repository;

import com.taskflow.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByName(String name);

    @Query("SELECT p FROM Permission p WHERE p.resourceType = :resourceType")
    Set<Permission> findByResourceType(String resourceType);

    @Query("SELECT p FROM Permission p WHERE p.resourceType = :resourceType AND p.actionType = :actionType")
    Set<Permission> findByResourceTypeAndActionType(String resourceType, String actionType);

    boolean existsByName(String name);
}
