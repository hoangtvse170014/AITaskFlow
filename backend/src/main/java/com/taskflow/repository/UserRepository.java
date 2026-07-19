package com.taskflow.repository;

import com.taskflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<User> searchUsers(@Param("query") String query);

    Optional<User> findByEmailAndProvider(String email, String provider);

    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    boolean existsByEmailAndProvider(String email, String provider);
}
